package com.eyeos.daemon.common.report;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

public class ReportWriter {

    public static final String INICIO = "=== EYEOS REPORT v1 ===";
    public static final String FIN = "=== END ===";

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public String escribir(EquipoReportado r, Set<SeccionReporte> secciones) {
        StringBuilder sb = new StringBuilder();
        sb.append(INICIO).append('\n');
        sb.append("TIMESTAMP=").append(r.getFecha() != null ? r.getFecha().format(FMT) : "").append('\n');
        sb.append("HOSTNAME=").append(limpiar(r.getHostname())).append('\n');
        sb.append("USER=").append(limpiar(r.getUsuario())).append('\n');
        sb.append("CONSENT=").append(limpiar(r.getConsentimiento())).append('\n');

        if (secciones.contains(SeccionReporte.SO)) {
            sb.append("[OS]\n");
            sb.append("NAME=").append(limpiar(r.getSoNombre())).append('\n');
            sb.append("VERSION=").append(limpiar(r.getSoVersion())).append('\n');
            sb.append("EDITION=").append(limpiar(r.getSoEdicion())).append('\n');
            sb.append("UPTIME_SECONDS=").append(r.getUptimeSegundos()).append('\n');
        }
        if (secciones.contains(SeccionReporte.MEMORIA)) {
            sb.append("[MEMORY]\n");
            sb.append("TOTAL_MB=").append(r.getRamTotalMb()).append('\n');
            sb.append("FREE_MB=").append(r.getRamLibreMb()).append('\n');
        }
        if (secciones.contains(SeccionReporte.RED)) {
            sb.append("[NETWORK]\n");
            for (String ip : r.getIps()) {
                sb.append("IP=").append(limpiar(ip)).append('\n');
            }
            for (String mac : r.getMacs()) {
                sb.append("MAC=").append(limpiar(mac)).append('\n');
            }
        }
        if (secciones.contains(SeccionReporte.HARDWARE)) {
            sb.append("[HARDWARE]\n");
            sb.append("BIOS_SERIAL=").append(limpiar(r.getBiosSerial())).append('\n');
            sb.append("BASEBOARD_UUID=").append(limpiar(r.getBaseboardUuid())).append('\n');
        }
        if (secciones.contains(SeccionReporte.PROCESOS)) {
            sb.append("[PROCESSES]\n");
            sb.append("PID|NAME|CPU%|MEM_MB\n");
            for (ProcesoInfo p : r.getProcesos()) {
                sb.append(p.getPid()).append('|')
                        .append(limpiar(p.getNombre())).append('|')
                        .append(String.format(Locale.US, "%.1f", p.getCpuPct())).append('|')
                        .append(p.getMemMb()).append('\n');
            }
        }
        if (secciones.contains(SeccionReporte.CONEXIONES)) {
            sb.append("[CONNECTIONS]\n");
            sb.append("STATE|PROTO|PORT|REMOTE|REMOTE_PORT\n");
            for (ConexionRed c : r.getConexiones()) {
                sb.append(limpiar(c.getEstado())).append('|')
                        .append(limpiar(c.getProtocolo())).append('|')
                        .append(c.getPuerto()).append('|')
                        .append(limpiar(c.getDireccionRemota())).append('|')
                        .append(c.getPuertoRemoto()).append('\n');
            }
        }
        if (secciones.contains(SeccionReporte.ACTIVIDAD)) {
            sb.append("[ACTIVITY]\n");
            sb.append("FOREGROUND=").append(limpiar(r.getForegroundApp())).append('\n');
            sb.append("FOREGROUND_PID=").append(r.getForegroundPid()).append('\n');
        }
        sb.append(FIN).append('\n');
        return sb.toString();
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.replace("\r", " ").replace("\n", " ").trim();
    }
}
