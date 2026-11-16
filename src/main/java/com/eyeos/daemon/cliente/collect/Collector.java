package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;

public interface Collector {

    SeccionReporte seccion();

    void recolectar(EquipoReportado reporte, ReportOptions options) throws Exception;
}
