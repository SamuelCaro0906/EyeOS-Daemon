package com.eyeos.daemon.cliente.config;

import com.eyeos.daemon.common.report.ReportOptions;

public class AgentConfig {

    private String ftpHost = "";
    private int ftpPort = 21;
    private String ftpUser = "";
    private String ftpPassword = "";
    private String remoteDir = "/EyeOS";
    private int intervaloSegundos = 25;
    private boolean terminarProcesos = false;
    private boolean recordarConsentimiento = true;
    private boolean autostart = false;
    private boolean habilitada = true;

    private final ReportOptions reportOptions = new ReportOptions();

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    public String getFtpUser() {
        return ftpUser;
    }

    public void setFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String getRemoteDir() {
        return remoteDir;
    }

    public void setRemoteDir(String remoteDir) {
        this.remoteDir = remoteDir;
    }

    public int getIntervaloSegundos() {
        return intervaloSegundos;
    }

    public void setIntervaloSegundos(int intervaloSegundos) {
        this.intervaloSegundos = Math.max(5, intervaloSegundos);
    }

    public boolean isTerminarProcesos() {
        return terminarProcesos;
    }

    public void setTerminarProcesos(boolean terminarProcesos) {
        this.terminarProcesos = terminarProcesos;
    }

    public boolean isRecordarConsentimiento() {
        return recordarConsentimiento;
    }

    public void setRecordarConsentimiento(boolean recordarConsentimiento) {
        this.recordarConsentimiento = recordarConsentimiento;
    }

    public boolean isAutostart() {
        return autostart;
    }

    public void setAutostart(boolean autostart) {
        this.autostart = autostart;
    }

    public boolean isHabilitada() {
        return habilitada;
    }

    public void setHabilitada(boolean habilitada) {
        this.habilitada = habilitada;
    }

    public ReportOptions getReportOptions() {
        return reportOptions;
    }

    public boolean configuracionCompleta() {
        return ftpHost != null && !ftpHost.isBlank()
                && ftpUser != null && !ftpUser.isBlank();
    }
}
