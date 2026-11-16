package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

import java.util.LinkedHashSet;
import java.util.Set;

public class NetworkCollector implements Collector {

    private final HardwareAbstractionLayer hal;

    public NetworkCollector(HardwareAbstractionLayer hal) {
        this.hal = hal;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.RED;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        Set<String> ips = new LinkedHashSet<>();
        Set<String> macs = new LinkedHashSet<>();

        for (NetworkIF netIf : hal.getNetworkIFs()) {
            if (netIf.getName() != null && netIf.getName().toLowerCase().contains("loopback")) {
                continue;
            }
            for (String ip : netIf.getIPv4addr()) {
                if (ip != null && !ip.isBlank()) {
                    ips.add(ip);
                }
            }
            String mac = netIf.getMacaddr();
            if (mac != null && !mac.isBlank() && !mac.equals("00:00:00:00:00:00")) {
                macs.add(mac.toUpperCase());
            }
        }

        reporte.getIps().addAll(ips);
        reporte.getMacs().addAll(macs);
    }
}
