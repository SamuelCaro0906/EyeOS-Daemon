package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.hardware.ComputerSystem;
import oshi.hardware.HardwareAbstractionLayer;

public class HardwareCollector implements Collector {

    private final HardwareAbstractionLayer hal;

    public HardwareCollector(HardwareAbstractionLayer hal) {
        this.hal = hal;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.HARDWARE;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        ComputerSystem cs = hal.getComputerSystem();
        reporte.setBiosSerial(valor(cs.getSerialNumber(), "N/D"));

        String uuid = cs.getHardwareUUID();
        if (uuid == null || uuid.isBlank()) {
            uuid = cs.getBaseboard() != null ? cs.getBaseboard().getSerialNumber() : null;
        }
        reporte.setBaseboardUuid(valor(uuid, "N/D"));
    }

    private String valor(String v, String porDefecto) {
        return v == null || v.isBlank() ? porDefecto : v;
    }
}
