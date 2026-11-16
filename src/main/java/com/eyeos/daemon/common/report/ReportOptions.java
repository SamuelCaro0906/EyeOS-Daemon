package com.eyeos.daemon.common.report;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class ReportOptions {

    private final Set<SeccionReporte> secciones = EnumSet.allOf(SeccionReporte.class);
    private final List<String> carpetas = new ArrayList<>();
    private int maxProcesos = 15;
    private long umbralProcesoMb = 600;

    public Set<SeccionReporte> getSecciones() {
        return secciones;
    }

    public void habilitarSeccion(SeccionReporte seccion, boolean habilitada) {
        if (habilitada) {
            secciones.add(seccion);
        } else {
            secciones.remove(seccion);
        }
    }

    public boolean incluye(SeccionReporte seccion) {
        return secciones.contains(seccion);
    }

    public List<String> getCarpetas() {
        return carpetas;
    }

    public int getMaxProcesos() {
        return maxProcesos;
    }

    public void setMaxProcesos(int maxProcesos) {
        this.maxProcesos = Math.max(1, maxProcesos);
    }

    public long getUmbralProcesoMb() {
        return umbralProcesoMb;
    }

    public void setUmbralProcesoMb(long umbralProcesoMb) {
        this.umbralProcesoMb = umbralProcesoMb;
    }
}
