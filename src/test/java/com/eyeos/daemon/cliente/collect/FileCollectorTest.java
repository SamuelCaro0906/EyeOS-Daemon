package com.eyeos.daemon.cliente.collect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCollectorTest {

    @TempDir
    Path temp;

    @Test
    void recolectaArchivosVisiblesRecursivamente() throws IOException {
        Files.writeString(temp.resolve("a.txt"), "hola");
        Files.writeString(temp.resolve(".oculto.txt"), "x");
        Path sub = Files.createDirectory(temp.resolve("sub"));
        Files.writeString(sub.resolve("b.log"), "mundo");

        List<Path> encontrados = new FileCollector().recolectar(List.of(temp.toString()));

        assertEquals(2, encontrados.size());
        assertTrue(encontrados.stream().anyMatch(p -> p.getFileName().toString().equals("a.txt")));
        assertTrue(encontrados.stream().anyMatch(p -> p.getFileName().toString().equals("b.log")));
    }

    @Test
    void carpetaInexistenteNoRompe() {
        List<Path> encontrados = new FileCollector().recolectar(List.of("Z:\\ruta\\que\\no\\existe"));
        assertTrue(encontrados.isEmpty());
    }
}
