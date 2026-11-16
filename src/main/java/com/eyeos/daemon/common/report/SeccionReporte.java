package com.eyeos.daemon.common.report;

public enum SeccionReporte {

    SO("Sistema Operativo"),
    MEMORIA("Memoria RAM"),
    RED("Red IP/MAC"),
    HARDWARE("Hardware BIOS/UUID"),
    PROCESOS("Procesos"),
    CONEXIONES("Conexiones de red"),
    ACTIVIDAD("Aplicación en uso");

    private final String etiqueta;

    SeccionReporte(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
