package com.eyeos.daemon.cliente.startup;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutostartManager {

    private static final String NOMBRE_TAREA = "EyeOSDaemon";

    public boolean instalar(String comando) {
        return ejecutar(List.of("schtasks", "/create",
                "/tn", NOMBRE_TAREA,
                "/tr", comando,
                "/sc", "onlogon",
                "/f"));
    }

    public boolean desinstalar() {
        return ejecutar(List.of("schtasks", "/delete", "/tn", NOMBRE_TAREA, "/f"));
    }

    public boolean estaInstalado() {
        return ejecutar(List.of("schtasks", "/query", "/tn", NOMBRE_TAREA));
    }

    private boolean ejecutar(List<String> comando) {
        try {
            Process process = new ProcessBuilder(comando)
                    .redirectErrorStream(true)
                    .start();
            boolean terminado = process.waitFor(15, TimeUnit.SECONDS);
            if (!terminado) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            System.err.println("[EyeOS] No se pudo gestionar el autostart: " + e.getMessage());
            return false;
        }
    }
}
