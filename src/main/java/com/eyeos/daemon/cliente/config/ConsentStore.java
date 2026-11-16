package com.eyeos.daemon.cliente.config;

import com.eyeos.daemon.common.report.SeccionReporte;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.TreeSet;

public class ConsentStore {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final String TEXTO_HASH = "sha256:";

    public static Path rutaConsentimiento() {
        return ConfigStore.carpetaAplicacion().resolve("consent.properties");
    }

    public boolean tieneConsentimiento(AgentConfig cfg) {
        Properties props = leer();
        if (!Boolean.parseBoolean(props.getProperty("registrado", "false"))) {
            return false;
        }
        String token = props.getProperty("token", "");
        return !token.isBlank() && token.endsWith(TEXTO_HASH + huella(cfg));
    }

    public String tokenActual() {
        return leer().getProperty("token", "");
    }

    public String registrar(AgentConfig cfg) {
        String token = LocalDateTime.now().format(FMT) + "|" + TEXTO_HASH + huella(cfg);
        Properties props = new Properties();
        props.setProperty("registrado", "true");
        props.setProperty("token", token);
        escribir(props);
        return token;
    }

    public void revocar() {
        Properties props = new Properties();
        props.setProperty("registrado", "false");
        props.setProperty("token", "");
        escribir(props);
    }

    private String huella(AgentConfig cfg) {
        StringBuilder sb = new StringBuilder();
        sb.append("v2|");
        sb.append(cfg.getFtpHost()).append('|').append(cfg.getRemoteDir()).append('|');
        TreeSet<String> secciones = new TreeSet<>();
        for (SeccionReporte seccion : cfg.getReportOptions().getSecciones()) {
            secciones.add(seccion.name());
        }
        StringJoiner sj = new StringJoiner(",");
        secciones.forEach(sj::add);
        sb.append(sj).append('|');
        cfg.getReportOptions().getCarpetas().stream().sorted().forEach(c -> sb.append(c).append(';'));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "sin-hash";
        }
    }

    private Properties leer() {
        Properties props = new Properties();
        Path ruta = rutaConsentimiento();
        if (!Files.exists(ruta)) {
            return props;
        }
        try (InputStream in = Files.newInputStream(ruta)) {
            props.load(in);
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo leer el consentimiento: " + e.getMessage());
        }
        return props;
    }

    private void escribir(Properties props) {
        try {
            Files.createDirectories(ConfigStore.carpetaAplicacion());
            try (OutputStream out = Files.newOutputStream(rutaConsentimiento())) {
                props.store(out, "EyeOS Daemon - Consentimiento");
            }
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo guardar el consentimiento: " + e.getMessage());
        }
    }
}
