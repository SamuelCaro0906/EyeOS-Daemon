package com.eyeos.daemon.cliente.guard;

import com.eyeos.daemon.common.model.ProcesoInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ProcessGuard {

    private static final Set<String> PROTEGIDOS = Set.of(
            "system", "registry", "smss.exe", "csrss.exe", "wininit.exe", "winlogon.exe",
            "services.exe", "lsass.exe", "svchost.exe", "dwm.exe", "explorer.exe",
            "taskhostw.exe", "java.exe", "javaw.exe", "conhost.exe"
    );

    private final long umbralMb;
    private final boolean terminar;
    private final long propioPid;

    public ProcessGuard(long umbralMb, boolean terminar) {
        this.umbralMb = umbralMb;
        this.terminar = terminar;
        this.propioPid = ProcessHandle.current().pid();
    }

    public List<String> evaluar(List<ProcesoInfo> procesos) {
        List<String> acciones = new ArrayList<>();
        for (ProcesoInfo proceso : procesos) {
            if (proceso.getMemMb() < umbralMb) {
                continue;
            }
            if (proceso.getPid() == propioPid || estaProtegido(proceso.getNombre())) {
                continue;
            }
            if (!terminar) {
                acciones.add("ALERTA: " + proceso.getNombre() + " usa " + proceso.getMemMb() + " MB");
                continue;
            }
            boolean ok = terminarProceso(proceso.getPid());
            acciones.add((ok ? "TERMINADO: " : "FALLO: ") + proceso.getNombre() + " (" + proceso.getMemMb() + " MB)");
        }
        return acciones;
    }

    private boolean estaProtegido(String nombre) {
        if (nombre == null) {
            return true;
        }
        return PROTEGIDOS.contains(nombre.toLowerCase());
    }

    private boolean terminarProceso(int pid) {
        Optional<ProcessHandle> handle = ProcessHandle.of(pid);
        if (handle.isEmpty() || !handle.get().isAlive()) {
            return false;
        }
        ProcessHandle proceso = handle.get();
        proceso.destroy();
        try {
            proceso.onExit().get(2, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
        if (proceso.isAlive()) {
            proceso.destroyForcibly();
        }
        return true;
    }
}
