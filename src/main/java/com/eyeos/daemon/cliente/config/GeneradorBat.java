package com.eyeos.daemon.cliente.config;

import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class GeneradorBat {

    public static Path carpetaScripts() {
        return ConfigStore.carpetaAplicacion().resolve("scripts");
    }

    public static Path rutaBat() {
        return carpetaScripts().resolve("generar_reporte.bat");
    }

    public static Path generar(AgentConfig config, String consent) throws IOException {
        Files.createDirectories(carpetaScripts());
        Files.writeString(rutaBat(), contenidoBat(), StandardCharsets.UTF_8);
        Files.writeString(carpetaScripts().resolve("colectar.ps1"),
                contenidoPs(config.getReportOptions(), consent), StandardCharsets.UTF_8);
        return rutaBat();
    }

    private static String contenidoBat() {
        return """
                @echo off
                setlocal
                if "%~1"=="" (
                  echo Uso: generar_reporte.bat "ruta\\salida.txt"
                  exit /b 1
                )
                powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0colectar.ps1" "%~1"
                exit /b %ERRORLEVEL%
                """;
    }

    private static String contenidoPs(ReportOptions options, String consent) {
        String token = consent == null ? "" : consent.replace("'", "''");
        StringBuilder sb = new StringBuilder();
        sb.append("""
                param([string]$Salida)
                $ErrorActionPreference = 'SilentlyContinue'
                function L($v) { if ($null -eq $v) { return '' }; return ([string]$v).Replace("`r", ' ').Replace("`n", ' ').Trim() }
                $lineas = New-Object System.Collections.Generic.List[string]
                $os = Get-CimInstance Win32_OperatingSystem
                $cs = Get-CimInstance Win32_ComputerSystem
                $lineas.Add('=== EYEOS REPORT v1 ===')
                $lineas.Add('TIMESTAMP=' + (Get-Date -Format 'yyyy-MM-ddTHH:mm:ss'))
                $lineas.Add('HOSTNAME=' + (L $env:COMPUTERNAME))
                $lineas.Add('USER=' + (L $env:USERNAME))
                $lineas.Add('CONSENT=' + (L '%s'))
                
                """.formatted(token));

        if (options.incluye(SeccionReporte.SO)) {
            sb.append("""
                    $lineas.Add('[OS]')
                    $lineas.Add('NAME=' + (L $os.Caption))
                    $lineas.Add('VERSION=' + (L $os.Version) + ' (build ' + (L $os.BuildNumber) + ')')
                    $lineas.Add('EDITION=' + (L $os.OSArchitecture) + ' - ' + (L $os.Manufacturer))
                    $uptime = [int]((Get-Date) - $os.LastBootUpTime).TotalSeconds
                    $lineas.Add('UPTIME_SECONDS=' + $uptime)
                    
                    """);
        }

        if (options.incluye(SeccionReporte.MEMORIA)) {
            sb.append("""
                    $lineas.Add('[MEMORY]')
                    $lineas.Add('TOTAL_MB=' + [int]($cs.TotalPhysicalMemory / 1MB))
                    $lineas.Add('FREE_MB=' + [int]($os.FreePhysicalMemory / 1KB))
                    
                    """);
        }

        if (options.incluye(SeccionReporte.RED)) {
            sb.append("""
                    $ips = @(Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -ne '127.0.0.1' } | Select-Object -ExpandProperty IPAddress)
                    $macs = @(Get-NetAdapter | Where-Object { $_.MacAddress } | Select-Object -ExpandProperty MacAddress)
                    $lineas.Add('[NETWORK]')
                    foreach ($ip in $ips) { $lineas.Add('IP=' + (L $ip)) }
                    foreach ($mac in $macs) { $lineas.Add('MAC=' + (L $mac).ToUpperInvariant()) }
                    
                    """);
        }

        if (options.incluye(SeccionReporte.HARDWARE)) {
            sb.append("""
                    $bios = Get-CimInstance Win32_BIOS
                    $board = Get-CimInstance Win32_BaseBoard
                    $lineas.Add('[HARDWARE]')
                    $lineas.Add('BIOS_SERIAL=' + (L $bios.SerialNumber))
                    $uuid = $cs.UUID
                    if (-not $uuid) { $uuid = $board.SerialNumber }
                    $lineas.Add('BASEBOARD_UUID=' + (L $uuid))
                    
                    """);
        }

        if (options.incluye(SeccionReporte.PROCESOS)) {
            sb.append("""
                    $lineas.Add('[PROCESSES]')
                    $lineas.Add('PID|NAME|CPU%|MEM_MB')
                    $procs = Get-Process | Sort-Object WS -Descending | Select-Object -First 15
                    foreach ($p in $procs) {
                      $cpu = if ($null -eq $p.CPU) { 0 } else { [math]::Round($p.CPU, 1) }
                      $cpuTexto = $cpu.ToString([Globalization.CultureInfo]::InvariantCulture)
                      $mem = [int]($p.WS / 1MB)
                      $nombre = (L $p.ProcessName).Replace('|', '-')
                      $lineas.Add(('{0}|{1}|{2}|{3}' -f $p.Id, $nombre, $cpuTexto, $mem))
                    }
                    
                    """);
        }

        if (options.incluye(SeccionReporte.CONEXIONES)) {
            sb.append("""
                    $lineas.Add('[CONNECTIONS]')
                    $lineas.Add('STATE|PROTO|PORT|REMOTE|REMOTE_PORT')
                    $conns = Get-NetTCPConnection | Where-Object { $_.State -in @('Listen', 'Established') } | Select-Object -First 100
                    foreach ($c in $conns) {
                      $remoto = ''
                      $puertoRemoto = 0
                      if ($c.State -eq 'Established') { $remoto = L $c.RemoteAddress; $puertoRemoto = $c.RemotePort }
                      $lineas.Add(('{0}|{1}|{2}|{3}|{4}' -f $c.State.ToString().ToUpperInvariant(), 'tcp4', $c.LocalPort, $remoto, $puertoRemoto))
                    }
                    
                    """);
        }

        if (options.incluye(SeccionReporte.ACTIVIDAD)) {
            sb.append("""
                    Add-Type -TypeDefinition @'
                    using System;
                    using System.Runtime.InteropServices;
                    public static class EyeosWin32 {
                      [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
                      [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint pid);
                    }
                    '@
                    $pidForeground = 0
                    $hwnd = [EyeosWin32]::GetForegroundWindow()
                    if ($hwnd -ne [IntPtr]::Zero) {
                      [uint32]$pidValue = 0
                      [void][EyeosWin32]::GetWindowThreadProcessId($hwnd, [ref]$pidValue)
                      $pidForeground = [int]$pidValue
                    }
                    $nombre = ''
                    if ($pidForeground -gt 0) {
                      $proc = Get-Process -Id $pidForeground
                      if ($proc) { $nombre = $proc.ProcessName }
                    }
                    $lineas.Add('[ACTIVITY]')
                    $lineas.Add('FOREGROUND=' + (L $nombre))
                    $lineas.Add('FOREGROUND_PID=' + $pidForeground)
                    
                    """);
        }

        sb.append("""
                $lineas.Add('=== END ===')
                [System.IO.File]::WriteAllLines($Salida, $lineas, [System.Text.UTF8Encoding]::new($false))
                """);
        return sb.toString();
    }
}
