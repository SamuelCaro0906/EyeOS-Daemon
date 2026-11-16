package com.eyeos.daemon.common.report;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportRoundTripTest {

    @Test
    void escribirYParsearConservaLosDatos() {
        EquipoReportado original = new EquipoReportado();
        original.setHostname("PC-TEST");
        original.setUsuario("tester");
        original.setConsentimiento("2026-11-14T10:00:00|sha256:abc123");
        original.setSoNombre("Windows 11");
        original.setSoVersion("10.0 (build 22631)");
        original.setSoEdicion("Windows - Microsoft");
        original.setRamTotalMb(16384);
        original.setRamLibreMb(8192);
        original.setBiosSerial("SN-123");
        original.setBaseboardUuid("UUID-ABC");
        original.setUptimeSegundos(18_000);
        original.setForegroundApp("Code.exe");
        original.setForegroundPid(4321);
        original.setFecha(LocalDateTime.of(2026, 11, 14, 10, 30, 0));
        original.getIps().add("192.168.0.10");
        original.getIps().add("10.0.0.5");
        original.getMacs().add("AA:BB:CC:DD:EE:FF");
        original.getProcesos().add(new ProcesoInfo(123, "chrome.exe", 12.5, 812));
        original.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 51515, "8.8.8.8", 443));

        String texto = new ReportWriter().escribir(original, EnumSet.allOf(SeccionReporte.class));
        EquipoReportado parseado = new ReporteParser().parsear(texto);

        assertEquals("PC-TEST", parseado.getHostname());
        assertEquals("tester", parseado.getUsuario());
        assertEquals("Windows 11", parseado.getSoNombre());
        assertEquals(16384, parseado.getRamTotalMb());
        assertEquals(2, parseado.getIps().size());
        assertEquals(1, parseado.getMacs().size());
        assertEquals("SN-123", parseado.getBiosSerial());
        assertEquals("UUID-ABC", parseado.getBaseboardUuid());
        assertEquals(1, parseado.getProcesos().size());
        assertEquals("chrome.exe", parseado.getProcesos().get(0).getNombre());
        assertEquals(18_000, parseado.getUptimeSegundos());
        assertEquals("Code.exe", parseado.getForegroundApp());
        assertEquals(4321, parseado.getForegroundPid());
        assertEquals(1, parseado.getConexiones().size());
        assertEquals(51515, parseado.getConexiones().get(0).getPuerto());
        assertEquals("8.8.8.8", parseado.getConexiones().get(0).getDireccionRemota());
        assertEquals(443, parseado.getConexiones().get(0).getPuertoRemoto());
    }

    @Test
    void parseaReporteAntiguoSinCamposNuevos() {
        String texto = """
                === EYEOS REPORT v1 ===
                TIMESTAMP=2026-11-14T10:00:00
                HOSTNAME=PC-OLD
                [OS]
                NAME=Windows 10
                [MEMORY]
                TOTAL_MB=8192
                FREE_MB=4096
                [CONNECTIONS]
                STATE|PROTO|PORT|REMOTE|REMOTE_PORT
                ESTABLISHED|TCP|5000
                === END ===
                """;

        EquipoReportado parseado = new ReporteParser().parsear(texto);

        assertEquals(0, parseado.getUptimeSegundos());
        assertEquals("", parseado.getForegroundApp());
        assertEquals(1, parseado.getConexiones().size());
        assertEquals(5000, parseado.getConexiones().get(0).getPuerto());
        assertEquals("", parseado.getConexiones().get(0).getDireccionRemota());
        assertEquals(0, parseado.getConexiones().get(0).getPuertoRemoto());
    }

    @Test
    void lasSeccionesDeshabilitadasNoSeEscriben() {
        EquipoReportado original = new EquipoReportado();
        original.setHostname("PC-X");
        original.setFecha(LocalDateTime.now());

        String texto = new ReportWriter().escribir(original, EnumSet.of(SeccionReporte.MEMORIA));

        assertTrue(texto.contains("[MEMORY]"));
        assertFalse(texto.contains("[PROCESSES]"));
        assertFalse(texto.contains("[NETWORK]"));
    }
}
