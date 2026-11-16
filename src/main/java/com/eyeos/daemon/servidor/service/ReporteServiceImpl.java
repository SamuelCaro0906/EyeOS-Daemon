package com.eyeos.daemon.servidor.service;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.servidor.dao.ReporteDAO;

import java.time.LocalDateTime;
import java.util.List;

public class ReporteServiceImpl implements ReporteService {

    private final ReporteDAO reporteDAO;

    public ReporteServiceImpl(ReporteDAO reporteDAO) {
        this.reporteDAO = reporteDAO;
    }

    @Override
    public void guardarReporte(EquipoReportado reporte) {
        if (reporte == null) {
            throw new IllegalArgumentException("El reporte no puede ser nulo.");
        }
        if (reporte.getHostname() == null || reporte.getHostname().isBlank()) {
            throw new IllegalArgumentException("El reporte debe tener un HOSTNAME.");
        }
        if (reporte.getFecha() == null) {
            reporte.setFecha(LocalDateTime.now());
        }
        reporteDAO.guardar(reporte);
    }

    @Override
    public List<EquipoReportado> listarReportes() {
        return reporteDAO.listarTodos();
    }

    @Override
    public List<EquipoReportado> listarPorEquipo(String hostname) {
        if (hostname == null || hostname.isBlank()) {
            throw new IllegalArgumentException("El hostname es obligatorio.");
        }
        return reporteDAO.listarPorEquipo(hostname);
    }
}
