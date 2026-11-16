package com.eyeos.daemon.cliente.collect;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileCollector {

    private static final long MAX_BYTES_POR_ARCHIVO = 20L * 1024L * 1024L;
    private static final int MAX_ARCHIVOS = 40;

    public List<Path> recolectar(List<String> carpetas) {
        List<Path> resultado = new ArrayList<>();
        for (String carpeta : carpetas) {
            if (resultado.size() >= MAX_ARCHIVOS) {
                break;
            }
            Path base = Paths.get(carpeta);
            if (!Files.isDirectory(base)) {
                System.err.println("[EyeOS] Carpeta invalida o inexistente: " + carpeta);
                continue;
            }
            try (Stream<Path> stream = Files.walk(base, 3)) {
                stream.filter(Files::isRegularFile)
                        .filter(this::esArchivoValido)
                        .sorted()
                        .limit(MAX_ARCHIVOS - resultado.size())
                        .forEach(resultado::add);
            } catch (IOException e) {
                System.err.println("[EyeOS] No se pudo leer la carpeta " + carpeta + ": " + e.getMessage());
            }
        }
        return resultado;
    }

    private boolean esArchivoValido(Path ruta) {
        try {
            String nombre = ruta.getFileName().toString().toLowerCase();
            if (nombre.startsWith(".")) {
                return false;
            }
            return Files.size(ruta) <= MAX_BYTES_POR_ARCHIVO;
        } catch (IOException e) {
            return false;
        }
    }
}
