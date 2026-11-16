package com.eyeos.daemon.common.model;

public class ProcesoInfo {

    private int pid;
    private String nombre;
    private double cpuPct;
    private long memMb;

    public ProcesoInfo() {
    }

    public ProcesoInfo(int pid, String nombre, double cpuPct, long memMb) {
        this.pid = pid;
        this.nombre = nombre;
        this.cpuPct = cpuPct;
        this.memMb = memMb;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getCpuPct() {
        return cpuPct;
    }

    public void setCpuPct(double cpuPct) {
        this.cpuPct = cpuPct;
    }

    public long getMemMb() {
        return memMb;
    }

    public void setMemMb(long memMb) {
        this.memMb = memMb;
    }

    @Override
    public String toString() {
        return "ProcesoInfo{" +
                "pid=" + pid +
                ", nombre='" + nombre + '\'' +
                ", cpuPct=" + cpuPct +
                ", memMb=" + memMb +
                '}';
    }
}
