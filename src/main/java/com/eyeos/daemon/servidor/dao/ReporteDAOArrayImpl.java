package com.eyeos.daemon.servidor.dao;

import com.eyeos.daemon.common.model.EquipoReportado;

import java.util.ArrayList;
import java.util.List;

public class ReporteDAOArrayImpl implements ReporteDAO {

    private final List<EquipoReportado> datos = new ArrayList<>();
    private int secuencia = 1;

    @Override
    public void guardar(EquipoReportado reporte) {
        reporte.setId(secuencia++);
        datos.add(reporte);
    }

    @Override
    public EquipoReportado buscarPorId(Integer id) {
        for (EquipoReportado reporte : datos) {
            if (reporte.getId() == id) {
                return reporte;
            }
        }
        return null;
    }

    @Override
    public List<EquipoReportado> listarTodos() {
        return new ArrayList<>(datos);
    }

    @Override
    public void actualizar(EquipoReportado reporte) {
        for (int i = 0; i < datos.size(); i++) {
            if (datos.get(i).getId() == reporte.getId()) {
                datos.set(i, reporte);
                return;
            }
        }
    }

    @Override
    public void eliminar(Integer id) {
        datos.removeIf(reporte -> reporte.getId() == id);
    }

    @Override
    public List<EquipoReportado> listarPorEquipo(String hostname) {
        List<EquipoReportado> resultado = new ArrayList<>();
        for (EquipoReportado reporte : datos) {
            if (reporte.getHostname() != null && reporte.getHostname().equalsIgnoreCase(hostname)) {
                resultado.add(reporte);
            }
        }
        return resultado;
    }
}
