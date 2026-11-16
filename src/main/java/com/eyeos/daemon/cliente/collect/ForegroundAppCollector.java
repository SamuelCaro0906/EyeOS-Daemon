package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

public class ForegroundAppCollector implements Collector {

    private final OperatingSystem os;
    private final boolean windows;

    public ForegroundAppCollector(OperatingSystem os) {
        this.os = os;
        this.windows = System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.ACTIVIDAD;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        if (!windows) {
            return;
        }
        try {
            WinDef.HWND ventana = User32.INSTANCE.GetForegroundWindow();
            if (ventana == null) {
                return;
            }
            IntByReference pid = new IntByReference();
            User32.INSTANCE.GetWindowThreadProcessId(ventana, pid);
            int idProceso = pid.getValue();
            if (idProceso <= 0) {
                return;
            }
            String nombre = "";
            OSProcess proceso = os.getProcess(idProceso);
            if (proceso != null && proceso.getName() != null) {
                nombre = proceso.getName();
            }
            reporte.setForegroundPid(idProceso);
            reporte.setForegroundApp(nombre);
        } catch (Throwable e) {
            System.err.println("[EyeOS] No se pudo leer la app en primer plano: " + e.getMessage());
        }
    }
}
