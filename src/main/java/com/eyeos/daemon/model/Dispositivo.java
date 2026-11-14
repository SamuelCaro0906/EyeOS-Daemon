package com.eyeos.daemon.model;

public class Dispositivo {
    private String codigoSerial;
    private String tipo;
    private String estado;

    public Dispositivo() {
    }

    public Dispositivo(String codigoSerial, String tipo, String estado) {
        this.codigoSerial = codigoSerial;
        this.tipo = tipo;
        this.estado = estado;
    }

    public String getCodigoSerial() {
        return codigoSerial;
    }

    public void setCodigoSerial(String codigoSerial) {
        this.codigoSerial = codigoSerial;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Dispositivo{" +
                "codigoSerial='" + codigoSerial + '\'' +
                ", tipo='" + tipo + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}