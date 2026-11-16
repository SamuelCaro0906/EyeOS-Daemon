package com.eyeos.daemon.cliente.ftp;

import com.eyeos.daemon.cliente.config.AgentConfig;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public class FtpUploader {

    public void subir(AgentConfig cfg, String hostname, String nombreReporte,
                      String contenidoReporte, List<Path> archivos) throws IOException {
        FTPClient ftp = new FTPClient();
        try {
            ftp.setConnectTimeout(15000);
            ftp.setDefaultTimeout(15000);
            ftp.connect(cfg.getFtpHost(), cfg.getFtpPort());

            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("Conexion FTP rechazada (codigo " + reply + ").");
            }
            if (!ftp.login(cfg.getFtpUser(), cfg.getFtpPassword())) {
                throw new IOException("Credenciales FTP invalidas: " + ftp.getReplyString().trim());
            }

            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            ftp.setDataTimeout(Duration.ofSeconds(30));
            ftp.setBufferSize(1024 * 1024);

            String hostDir = normalizar(cfg.getRemoteDir()) + "/" + limpiarNombre(hostname);
            asegurarDirectorio(ftp, hostDir);

            try (InputStream in = new ByteArrayInputStream(contenidoReporte.getBytes(StandardCharsets.UTF_8))) {
                if (!ftp.storeFile(nombreReporte, in)) {
                    throw new IOException("No se pudo subir el reporte: " + ftp.getReplyString().trim());
                }
            }

            if (archivos != null && !archivos.isEmpty()) {
                asegurarDirectorio(ftp, hostDir + "/archivos");
                for (Path archivo : archivos) {
                    String destino = System.currentTimeMillis() + "_" + limpiarNombre(archivo.getFileName().toString());
                    try (InputStream in = Files.newInputStream(archivo)) {
                        if (!ftp.storeFile(destino, in)) {
                            System.err.println("[EyeOS] No se pudo subir " + archivo.getFileName()
                                    + ": " + ftp.getReplyString().trim());
                        }
                    }
                }
            }
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.logout();
                } catch (IOException ignored) {
                }
                try {
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void asegurarDirectorio(FTPClient ftp, String ruta) throws IOException {
        ftp.changeWorkingDirectory("/");
        for (String parte : ruta.split("/")) {
            if (parte.isBlank()) {
                continue;
            }
            if (!ftp.changeWorkingDirectory(parte)) {
                ftp.makeDirectory(parte);
                if (!ftp.changeWorkingDirectory(parte)) {
                    throw new IOException("No se pudo crear o acceder al directorio remoto: " + parte);
                }
            }
        }
    }

    private String normalizar(String ruta) {
        String base = ruta == null || ruta.isBlank() ? "/EyeOS" : ruta.trim();
        if (!base.startsWith("/")) {
            base = "/" + base;
        }
        while (base.endsWith("/") && base.length() > 1) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String limpiarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "desconocido";
        }
        return nombre.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
