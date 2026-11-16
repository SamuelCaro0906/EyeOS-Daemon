package com.eyeos.daemon.servidor.service;

import com.eyeos.daemon.common.model.EquipoReportado;

import java.util.List;

public interface ReporteService {

    void guardarReporte(EquipoReportado reporte);

    List<EquipoReportado> listarReportes();

    List<EquipoReportado> listarPorEquipo(String hostname);
}
