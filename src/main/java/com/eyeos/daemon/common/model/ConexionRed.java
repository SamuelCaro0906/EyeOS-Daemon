package com.eyeos.daemon.common.model;

public class ConexionRed {

    private String estado;
    private String protocolo;
    private int puerto;
    private String direccionRemota = "";
    private int puertoRemoto;

    public ConexionRed() {
    }

    public ConexionRed(String estado, String protocolo, int puerto) {
        this.estado = estado;
        this.protocolo = protocolo;
        this.puerto = puerto;
    }

    public ConexionRed(String estado, String protocolo, int puerto,
                       String direccionRemota, int puertoRemoto) {
        this(estado, protocolo, puerto);
        this.direccionRemota = direccionRemota != null ? direccionRemota : "";
        this.puertoRemoto = puertoRemoto;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getProtocolo() {
        return protocolo;
    }

    public void setProtocolo(String protocolo) {
        this.protocolo = protocolo;
    }

    public int getPuerto() {
        return puerto;
    }

    public void setPuerto(int puerto) {
        this.puerto = puerto;
    }

    public String getDireccionRemota() {
        return direccionRemota;
    }

    public void setDireccionRemota(String direccionRemota) {
        this.direccionRemota = direccionRemota != null ? direccionRemota : "";
    }

    public int getPuertoRemoto() {
        return puertoRemoto;
    }

    public void setPuertoRemoto(int puertoRemoto) {
        this.puertoRemoto = puertoRemoto;
    }

    @Override
    public String toString() {
        return estado + "|" + protocolo + "|" + puerto + "|" + direccionRemota + "|" + puertoRemoto;
    }
}
