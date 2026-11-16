package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

public class MemoryCollector implements Collector {

    private static final long BYTES_EN_MB = 1024L * 1024L;

    private final HardwareAbstractionLayer hal;

    public MemoryCollector(HardwareAbstractionLayer hal) {
        this.hal = hal;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.MEMORIA;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        GlobalMemory memoria = hal.getMemory();
        reporte.setRamTotalMb(memoria.getTotal() / BYTES_EN_MB);
        reporte.setRamLibreMb(memoria.getAvailable() / BYTES_EN_MB);
    }
}
