package com.eyeos.daemon.cliente.config;

import com.eyeos.daemon.common.report.SeccionReporte;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;
import java.util.StringJoiner;

public class ConfigStore {

    public static Path carpetaAplicacion() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank()
                ? Paths.get(appData)
                : Paths.get(System.getProperty("user.home"));
        return base.resolve("EyeOS");
    }

    public static Path rutaConfig() {
        return carpetaAplicacion().resolve("config.properties");
    }

    public void guardar(AgentConfig cfg) throws IOException {
        Files.createDirectories(carpetaAplicacion());
        Properties props = new Properties();
        props.setProperty("ftp.host", valor(cfg.getFtpHost()));
        props.setProperty("ftp.port", String.valueOf(cfg.getFtpPort()));
        props.setProperty("ftp.user", valor(cfg.getFtpUser()));
        props.setProperty("ftp.password", Base64.getEncoder().encodeToString(
                valor(cfg.getFtpPassword()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        props.setProperty("ftp.remoteDir", valor(cfg.getRemoteDir()));
        props.setProperty("intervalo.segundos", String.valueOf(cfg.getIntervaloSegundos()));
        props.setProperty("procesos.terminar", String.valueOf(cfg.isTerminarProcesos()));
        props.setProperty("procesos.umbralMb", String.valueOf(cfg.getReportOptions().getUmbralProcesoMb()));
        props.setProperty("procesos.max", String.valueOf(cfg.getReportOptions().getMaxProcesos()));
        props.setProperty("consent.recordar", String.valueOf(cfg.isRecordarConsentimiento()));
        props.setProperty("autostart", String.valueOf(cfg.isAutostart()));
        props.setProperty("habilitada", String.valueOf(cfg.isHabilitada()));

        StringJoiner secciones = new StringJoiner(",");
        for (SeccionReporte seccion : cfg.getReportOptions().getSecciones()) {
            secciones.add(seccion.name());
        }
        props.setProperty("secciones", secciones.toString());
        props.setProperty("secciones.version", "2");

        StringJoiner carpetas = new StringJoiner("|");
        for (String carpeta : cfg.getReportOptions().getCarpetas()) {
            carpetas.add(carpeta);
        }
        props.setProperty("carpetas", carpetas.toString());

        try (OutputStream out = Files.newOutputStream(rutaConfig())) {
            props.store(out, "EyeOS Daemon");
        }
    }

    public AgentConfig cargar() {
        AgentConfig cfg = new AgentConfig();
        Path ruta = rutaConfig();
        if (!Files.exists(ruta)) {
            return cfg;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(ruta)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo leer la configuracion: " + e.getMessage());
            return cfg;
        }

        cfg.setFtpHost(props.getProperty("ftp.host", ""));
        cfg.setFtpPort(aInt(props.getProperty("ftp.port"), 21));
        cfg.setFtpUser(props.getProperty("ftp.user", ""));
        cfg.setFtpPassword(new String(Base64.getDecoder().decode(
                props.getProperty("ftp.password", "")), java.nio.charset.StandardCharsets.UTF_8));
        cfg.setRemoteDir(props.getProperty("ftp.remoteDir", "/EyeOS"));
        cfg.setIntervaloSegundos(aInt(props.getProperty("intervalo.segundos"), 25));
        cfg.setTerminarProcesos(Boolean.parseBoolean(props.getProperty("procesos.terminar", "false")));
        cfg.getReportOptions().setUmbralProcesoMb(aLong(props.getProperty("procesos.umbralMb"), 600));
        cfg.getReportOptions().setMaxProcesos(aInt(props.getProperty("procesos.max"), 15));
        cfg.setRecordarConsentimiento(Boolean.parseBoolean(props.getProperty("consent.recordar", "true")));
        cfg.setAutostart(Boolean.parseBoolean(props.getProperty("autostart", "false")));
        cfg.setHabilitada(Boolean.parseBoolean(props.getProperty("habilitada", "true")));

        String secciones = props.getProperty("secciones", "");
        if (!secciones.isBlank()) {
            for (SeccionReporte seccion : SeccionReporte.values()) {
                cfg.getReportOptions().habilitarSeccion(seccion, false);
            }
            for (String nombre : secciones.split(",")) {
                try {
                    cfg.getReportOptions().habilitarSeccion(SeccionReporte.valueOf(nombre.trim()), true);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        if (!"2".equals(props.getProperty("secciones.version"))) {
            cfg.getReportOptions().habilitarSeccion(SeccionReporte.ACTIVIDAD, true);
        }

        String carpetas = props.getProperty("carpetas", "");
        if (!carpetas.isBlank()) {
            for (String carpeta : carpetas.split("\\|")) {
                if (!carpeta.isBlank()) {
                    cfg.getReportOptions().getCarpetas().add(carpeta);
                }
            }
        }
        return cfg;
    }

    private String valor(String v) {
        return v != null ? v : "";
    }

    private int aInt(String v, int porDefecto) {
        try {
            return Integer.parseInt(v);
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private long aLong(String v, long porDefecto) {
        try {
            return Long.parseLong(v);
        } catch (Exception e) {
            return porDefecto;
        }
    }
}
