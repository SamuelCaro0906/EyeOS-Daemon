package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.software.os.OperatingSystem;

public class SystemCollector implements Collector {

    private final OperatingSystem os;

    public SystemCollector(OperatingSystem os) {
        this.os = os;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.SO;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        reporte.setUsuario(System.getProperty("user.name", "desconocido"));
        reporte.setHostname(resolverHostname());
        reporte.setSoNombre(System.getProperty("os.name", "Desconocido"));

        OperatingSystem.OSVersionInfo version = os.getVersionInfo();
        reporte.setSoVersion(version.getVersion() + " (build " + version.getBuildNumber() + ")");
        reporte.setSoEdicion(os.getFamily() + " - " + os.getManufacturer());
        reporte.setUptimeSegundos(os.getSystemUptime());
    }

    private String resolverHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            String env = System.getenv("COMPUTERNAME");
            return env != null ? env : "desconocido";
        }
    }
}
