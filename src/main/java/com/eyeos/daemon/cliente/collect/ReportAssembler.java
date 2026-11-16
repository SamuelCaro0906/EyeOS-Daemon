package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;

import java.time.LocalDateTime;
import java.util.List;

public class ReportAssembler {

    private final List<Collector> recolectores;

    public ReportAssembler() {
        SystemInfo systemInfo = new SystemInfo();
        OperatingSystem os = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hal = systemInfo.getHardware();

        this.recolectores = List.of(
                new SystemCollector(os),
                new MemoryCollector(hal),
                new NetworkCollector(hal),
                new HardwareCollector(hal),
                new ProcessCollector(os),
                new ConnectionCollector(os),
                new ForegroundAppCollector(os)
        );
    }

    public EquipoReportado ensamblar(ReportOptions options, String consentimiento) {
        EquipoReportado reporte = new EquipoReportado();
        reporte.setFecha(LocalDateTime.now());
        reporte.setConsentimiento(consentimiento);

        for (Collector recolector : recolectores) {
            if (recolector.seccion() != SeccionReporte.SO && !options.incluye(recolector.seccion())) {
                continue;
            }
            try {
                recolector.recolectar(reporte, options);
            } catch (Exception e) {
                System.err.println("[EyeOS] Fallo al recolectar " + recolector.seccion() + ": " + e.getMessage());
            }
        }
        return reporte;
    }
}
