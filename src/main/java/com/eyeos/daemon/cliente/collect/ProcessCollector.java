package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.util.Comparator;
import java.util.List;

public class ProcessCollector implements Collector {

    private static final long BYTES_EN_MB = 1024L * 1024L;

    private final OperatingSystem os;

    public ProcessCollector(OperatingSystem os) {
        this.os = os;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.PROCESOS;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        Comparator<OSProcess> porMemoria = Comparator.comparingLong(OSProcess::getResidentMemory).reversed();
        List<OSProcess> procesos = os.getProcesses(null, porMemoria, options.getMaxProcesos());

        for (OSProcess proceso : procesos) {
            ProcesoInfo info = new ProcesoInfo(
                    proceso.getProcessID(),
                    proceso.getName(),
                    proceso.getProcessCpuLoadCumulative() * 100.0,
                    proceso.getResidentMemory() / BYTES_EN_MB
            );
            reporte.getProcesos().add(info);
        }
    }
}
