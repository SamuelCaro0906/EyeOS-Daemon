package com.eyeos.daemon.common.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EquipoReportado {

    private int id;
    private String hostname;
    private String usuario;
    private String consentimiento;
    private String soNombre;
    private String soVersion;
    private String soEdicion;
    private long ramTotalMb;
    private long ramLibreMb;
    private String biosSerial;
    private String baseboardUuid;
    private long uptimeSegundos;
    private String foregroundApp = "";
    private int foregroundPid;
    private LocalDateTime fecha;
    private List<String> ips = new ArrayList<>();
    private List<String> macs = new ArrayList<>();
    private List<ConexionRed> conexiones = new ArrayList<>();
    private List<ProcesoInfo> procesos = new ArrayList<>();

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getConsentimiento() {
        return consentimiento;
    }

    public void setConsentimiento(String consentimiento) {
        this.consentimiento = consentimiento;
    }

    public String getSoNombre() {
        return soNombre;
    }

    public void setSoNombre(String soNombre) {
        this.soNombre = soNombre;
    }

    public String getSoVersion() {
        return soVersion;
    }

    public void setSoVersion(String soVersion) {
        this.soVersion = soVersion;
    }

    public String getSoEdicion() {
        return soEdicion;
    }

    public void setSoEdicion(String soEdicion) {
        this.soEdicion = soEdicion;
    }

    public long getRamTotalMb() {
        return ramTotalMb;
    }

    public void setRamTotalMb(long ramTotalMb) {
        this.ramTotalMb = ramTotalMb;
    }

    public long getRamLibreMb() {
        return ramLibreMb;
    }

    public void setRamLibreMb(long ramLibreMb) {
        this.ramLibreMb = ramLibreMb;
    }

    public String getBiosSerial() {
        return biosSerial;
    }

    public void setBiosSerial(String biosSerial) {
        this.biosSerial = biosSerial;
    }

    public String getBaseboardUuid() {
        return baseboardUuid;
    }

    public void setBaseboardUuid(String baseboardUuid) {
        this.baseboardUuid = baseboardUuid;
    }

    public long getUptimeSegundos() {
        return uptimeSegundos;
    }

    public void setUptimeSegundos(long uptimeSegundos) {
        this.uptimeSegundos = uptimeSegundos;
    }

    public String getForegroundApp() {
        return foregroundApp;
    }

    public void setForegroundApp(String foregroundApp) {
        this.foregroundApp = foregroundApp != null ? foregroundApp : "";
    }

    public int getForegroundPid() {
        return foregroundPid;
    }

    public void setForegroundPid(int foregroundPid) {
        this.foregroundPid = foregroundPid;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public List<String> getIps() {
        return ips;
    }

    public void setIps(List<String> ips) {
        this.ips = ips != null ? ips : new ArrayList<>();
    }

    public List<String> getMacs() {
        return macs;
    }

    public void setMacs(List<String> macs) {
        this.macs = macs != null ? macs : new ArrayList<>();
    }

    public List<ConexionRed> getConexiones() {
        return conexiones;
    }

    public void setConexiones(List<ConexionRed> conexiones) {
        this.conexiones = conexiones != null ? conexiones : new ArrayList<>();
    }

    public List<ProcesoInfo> getProcesos() {
        return procesos;
    }

    public void setProcesos(List<ProcesoInfo> procesos) {
        this.procesos = procesos != null ? procesos : new ArrayList<>();
    }

    @Override
    public String toString() {
        return "EquipoReportado{" +
                "hostname='" + hostname + '\'' +
                ", usuario='" + usuario + '\'' +
                ", soNombre='" + soNombre + '\'' +
                ", ramTotalMb=" + ramTotalMb +
                ", ips=" + ips +
                ", fecha=" + fecha +
                '}';
    }
}
