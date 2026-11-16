package com.eyeos.daemon.servidor.dao;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.dao.GenericDAO;

import java.util.List;

public interface ReporteDAO extends GenericDAO<EquipoReportado, Integer> {

    List<EquipoReportado> listarPorEquipo(String hostname);
}
