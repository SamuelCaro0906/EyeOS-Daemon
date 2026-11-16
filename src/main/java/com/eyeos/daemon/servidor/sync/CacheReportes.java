package com.eyeos.daemon.servidor.sync;

import com.eyeos.daemon.cliente.config.ConfigStore;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportWriter;
import com.eyeos.daemon.common.report.ReporteParser;
import com.eyeos.daemon.common.report.SeccionReporte;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CacheReportes {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int DIAS_RETENCION = 30;

    private final ReportWriter writer = new ReportWriter();
    private final ReporteParser parser = new ReporteParser();

    public Path carpeta() {
        return ConfigStore.carpetaAplicacion().resolve("reportes");
    }

    public List<EquipoReportado> cargar() {
        Path dir = carpeta();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        List<EquipoReportado> reportes = new ArrayList<>();
        List<Path> obsoletos = new ArrayList<>();
        LocalDateTime limite = LocalDateTime.now().minusDays(DIAS_RETENCION);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.txt")) {
            for (Path archivo : stream) {
                try {
                    EquipoReportado reporte = parser.parsear(Files.readString(archivo, StandardCharsets.UTF_8));
                    if (reporte.getFecha() != null && reporte.getFecha().isBefore(limite)) {
                        obsoletos.add(archivo);
                    } else {
                        reportes.add(reporte);
                    }
                } catch (Exception e) {
                    System.err.println("[EyeOS] Cache invalido " + archivo.getFileName() + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo leer la cache: " + e.getMessage());
            return List.of();
        }
        for (Path archivo : obsoletos) {
            try {
                Files.deleteIfExists(archivo);
            } catch (IOException ignored) {
            }
        }
        return reportes;
    }

    public void guardar(EquipoReportado reporte) {
        if (reporte == null || reporte.getFecha() == null) {
            return;
        }
        try {
            Path dir = carpeta();
            Files.createDirectories(dir);
            String host = limpiar(reporte.getHostname());
            String nombre = host + "_" + reporte.getFecha().format(FMT) + ".txt";
            Path destino = dir.resolve(nombre);
            if (Files.exists(destino)) {
                return;
            }
            String contenido = writer.escribir(reporte, Set.of(SeccionReporte.values()));
            Files.writeString(destino, contenido, StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo guardar en cache: " + e.getMessage());
        }
    }

    private String limpiar(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "equipo";
        }
        return nombre.replaceAll("[^A-Za-z0-9._-]", "_").toLowerCase(Locale.ROOT);
    }
}
