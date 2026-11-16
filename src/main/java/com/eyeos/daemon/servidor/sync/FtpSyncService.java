package com.eyeos.daemon.servidor.sync;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReporteParser;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class FtpSyncService {

    private static final int PROFUNDIDAD_MAXIMA = 5;
    private static final int MAX_ARCHIVOS = 2000;

    private final ReporteParser parser = new ReporteParser();
    private final Set<String> archivosCargados = ConcurrentHashMap.newKeySet();

    public List<EquipoReportado> sincronizar(String host, int puerto, String usuario,
                                             String clave, String remoteDir) throws IOException {
        List<EquipoReportado> reportes = new ArrayList<>();
        FTPClient ftp = new FTPClient();
        try {
            ftp.setConnectTimeout(15000);
            ftp.setDefaultTimeout(15000);
            ftp.connect(host, puerto);

            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                throw new IOException("Conexion FTP rechazada (codigo " + ftp.getReplyCode() + ").");
            }
            if (!ftp.login(usuario, clave)) {
                throw new IOException("Credenciales FTP invalidas: " + ftp.getReplyString().trim());
            }

            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.setDataTimeout(Duration.ofSeconds(30));

            String base = remoteDir == null || remoteDir.isBlank() ? "/" : remoteDir;
            List<String> rutas = new ArrayList<>();
            listarArchivos(ftp, base, rutas, 0);
            rutas.sort(Comparator.reverseOrder());
            List<String> nuevas = rutas.stream()
                    .filter(ruta -> !archivosCargados.contains(ruta))
                    .limit(MAX_ARCHIVOS)
                    .toList();
            for (String ruta : nuevas) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                if (!ftp.retrieveFile(ruta, buffer)) {
                    continue;
                }
                archivosCargados.add(ruta);
                try {
                    reportes.add(parser.parsear(buffer.toString(StandardCharsets.UTF_8)));
                } catch (Exception e) {
                    System.err.println("[EyeOS] Reporte invalido " + ruta + ": " + e.getMessage());
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
        return reportes;
    }

    private void listarArchivos(FTPClient ftp, String directorio,
                                List<String> rutas, int profundidad) throws IOException {
        if (profundidad > PROFUNDIDAD_MAXIMA) {
            return;
        }
        FTPFile[] archivos = ftp.listFiles(directorio);
        for (FTPFile archivo : archivos) {
            String ruta = unir(directorio, archivo.getName());
            if (archivo.isDirectory()) {
                if (!archivo.getName().equals(".") && !archivo.getName().equals("..")) {
                    listarArchivos(ftp, ruta, rutas, profundidad + 1);
                }
            } else if (archivo.isFile() && archivo.getName().toLowerCase().endsWith(".txt")) {
                rutas.add(ruta);
            }
        }
    }

    private String unir(String directorio, String nombre) {
        if (directorio.endsWith("/")) {
            return directorio + nombre;
        }
        return directorio + "/" + nombre;
    }
}
