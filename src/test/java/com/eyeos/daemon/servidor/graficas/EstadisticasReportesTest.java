package com.eyeos.daemon.servidor.graficas;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadisticasReportesTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 14, 10, 0, 0);

    @Test
    void deduplicarEliminaRepetidosYOrdenaAscendente() {
        EquipoReportado primero = base("PC-A", T0);
        EquipoReportado repetido = base("PC-A", T0);
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));

        List<EquipoReportado> unicos =
                EstadisticasReportes.deduplicar(List.of(segundo, primero, repetido));

        assertEquals(2, unicos.size());
        assertEquals(T0, unicos.get(0).getFecha());
        assertEquals(T0.plusSeconds(25), unicos.get(1).getFecha());
        assertEquals(List.of("PC-A"), EstadisticasReportes.hostnames(unicos));
    }

    @Test
    void seriesRamCalculaPorcentajeYUsada() {
        EquipoReportado primero = base("PC-A", T0);
        primero.setRamLibreMb(400);
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));
        segundo.setRamLibreMb(700);

        Map<String, List<EstadisticasReportes.PuntoRam>> series =
                EstadisticasReportes.seriesRam(List.of(primero, segundo), "PC-A");

        List<EstadisticasReportes.PuntoRam> puntos = series.get("PC-A");
        assertEquals(2, puntos.size());
        assertEquals(60.0, puntos.get(0).pct(), 0.01);
        assertEquals(600, puntos.get(0).usadaMb());
        assertEquals(30.0, puntos.get(1).pct(), 0.01);

        EstadisticasReportes.ResumenRam resumen =
                EstadisticasReportes.resumenRam(List.of(primero, segundo), "PC-A");
        assertEquals(45.0, resumen.promedioPct(), 0.01);
        assertEquals(60.0, resumen.maximaPct(), 0.01);
        assertEquals(2, resumen.muestras());
    }

    @Test
    void sesionesSeparanHuecosMayoresAlUmbral() {
        List<EquipoReportado> reportes = List.of(
                base("PC-A", T0),
                base("PC-A", T0.plusSeconds(25)),
                base("PC-A", T0.plusSeconds(50)),
                base("PC-A", T0.plusSeconds(200)),
                base("PC-A", T0.plusSeconds(225)));

        List<EstadisticasReportes.Sesion> sesiones =
                EstadisticasReportes.sesiones(reportes, "PC-A");

        assertEquals(2, sesiones.size());
        assertEquals(50 / 60.0, sesiones.get(0).minutos(), 0.01);
        assertEquals(25 / 60.0, sesiones.get(1).minutos(), 0.01);

        java.util.List<EquipoReportado> largos = new java.util.ArrayList<>();
        for (int i = 0; i <= 120; i++) {
            largos.add(base("PC-A", T0.plusSeconds(i * 25L)));
        }
        List<EstadisticasReportes.UsoDia> dias =
                EstadisticasReportes.tiempoUsoPorDia(largos, "PC-A");
        assertEquals(1, dias.size());
        assertEquals(0.83, dias.get(0).horas(), 0.01);

        assertEquals(2, EstadisticasReportes.resumenUso(reportes, "PC-A").sesiones());
    }

    @Test
    void horasInicioUsaArranqueYEvitaRepetidos() {
        EquipoReportado primero = base("PC-A", LocalDateTime.of(2026, 11, 14, 10, 30));
        primero.setUptimeSegundos(7200);
        EquipoReportado segundo = base("PC-A", LocalDateTime.of(2026, 11, 14, 10, 35));
        segundo.setUptimeSegundos(7500);
        EquipoReportado tercero = base("PC-B", LocalDateTime.of(2026, 11, 14, 18, 5));
        tercero.setUptimeSegundos(3600);

        List<EstadisticasReportes.Conteo> horas =
                EstadisticasReportes.horasInicio(List.of(primero, segundo, tercero), "Todos");

        assertEquals(24, horas.size());
        assertEquals(1, horas.get(8).valor());
        assertEquals(1, horas.get(17).valor());

        EstadisticasReportes.ResumenInicio resumen =
                EstadisticasReportes.resumenInicio(List.of(primero, segundo, tercero), "Todos");
        assertEquals(2, resumen.registros());
        assertEquals(8, resumen.horaMasComun());
        assertEquals(12.5, resumen.horaPromedio(), 0.01);
    }

    @Test
    void appsMasUsadasUsaPrimerPlanoYDescartaIdle() {
        EquipoReportado primero = base("PC-A", T0);
        primero.setForegroundApp("chrome.exe");
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));
        segundo.setForegroundApp("chrome.exe");
        EquipoReportado tercero = base("PC-A", T0.plusSeconds(50));
        tercero.setForegroundApp("Idle");
        EquipoReportado cuarto = base("PC-A", T0.plusSeconds(200));
        cuarto.setForegroundApp("notepad.exe");
        EquipoReportado quinto = base("PC-A", T0.plusSeconds(225));
        quinto.setForegroundApp("notepad.exe");
        EquipoReportado sexto = base("PC-A", T0.plusSeconds(250));
        sexto.setForegroundApp("notepad.exe");

        List<EstadisticasReportes.AppUso> apps = EstadisticasReportes.appsMasUsadas(
                List.of(primero, segundo, tercero, cuarto, quinto, sexto), "PC-A");

        assertEquals(2, apps.size());
        assertEquals("notepad", apps.get(0).app());
        assertEquals(1.3, apps.get(0).minutos(), 0.01);
        assertEquals("chrome", apps.get(1).app());
    }

    @Test
    void appsMasUsadasSinPrimerPlanoUsaProcesos() {
        EquipoReportado primero = base("PC-A", T0);
        primero.getProcesos().add(new ProcesoInfo(1, "chrome.exe", 1, 100));
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));
        segundo.getProcesos().add(new ProcesoInfo(1, "chrome.exe", 1, 100));
        EquipoReportado tercero = base("PC-A", T0.plusSeconds(50));
        tercero.getProcesos().add(new ProcesoInfo(2, "winword.exe", 1, 100));

        List<EstadisticasReportes.AppUso> apps =
                EstadisticasReportes.appsMasUsadas(List.of(primero, segundo, tercero), "PC-A");

        assertEquals("chrome", apps.get(0).app());
        assertEquals(0.8, apps.get(0).minutos(), 0.01);
        assertEquals("winword", apps.get(1).app());
    }

    @Test
    void appsMasUsadasMezclaPrimerPlanoYProcesos() {
        EquipoReportado viejo = base("PC-A", T0);
        viejo.getProcesos().add(new ProcesoInfo(1, "idea64.exe", 1, 900));
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));
        segundo.setForegroundApp("chrome.exe");
        EquipoReportado tercero = base("PC-A", T0.plusSeconds(50));
        tercero.setForegroundApp("chrome.exe");

        List<EstadisticasReportes.AppUso> apps =
                EstadisticasReportes.appsMasUsadas(List.of(viejo, segundo, tercero), "PC-A");

        assertEquals("chrome", apps.get(0).app());
        assertEquals(0.8, apps.get(0).minutos(), 0.01);
        assertEquals("idea64", apps.get(1).app());
        assertEquals(0.4, apps.get(1).minutos(), 0.01);
    }

    @Test
    void coberturaCuentaActividadYArranque() {
        EquipoReportado conDatos = base("PC-A", T0);
        conDatos.setForegroundApp("chrome");
        conDatos.setUptimeSegundos(7200);
        EquipoReportado sinDatos = base("PC-A", T0.plusSeconds(25));

        EstadisticasReportes.Cobertura cobertura =
                EstadisticasReportes.cobertura(List.of(conDatos, sinDatos), "PC-A");

        assertEquals(2, cobertura.total());
        assertEquals(1, cobertura.conActividad());
        assertEquals(1, cobertura.conArranque());
    }

    @Test
    void textoRamFormateaTotalLargoSinError() {
        String texto = PanelEstadisticas.textoRam(
                new EstadisticasReportes.ResumenRam(50.0, 60.0, 16071, 3));
        assertTrue(texto.contains("16071 MB"));
        assertEquals("Sin datos de RAM", PanelEstadisticas.textoRam(
                new EstadisticasReportes.ResumenRam(0, 0, 0, 0)));
    }

    @Test
    void dominiosIgnoraPrivadasYRespetaSoloWeb() {
        EquipoReportado primero = base("PC-A", T0);
        primero.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 50000, "8.8.8.8", 443));
        primero.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 50001, "192.168.1.5", 443));
        primero.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 50002, "1.1.1.1", 22));
        EquipoReportado segundo = base("PC-A", T0.plusSeconds(25));
        segundo.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 50003, "8.8.8.8", 443));
        segundo.getConexiones().add(new ConexionRed("ESTABLISHED", "TCP", 50004, "8.8.8.8", 443));
        segundo.getConexiones().add(new ConexionRed("LISTENING", "TCP", 50005, "9.9.9.9", 443));

        ResolverDominios resolver = new ResolverDominios(ip -> switch (ip) {
            case "8.8.8.8" -> "dns.google.";
            case "1.1.1.1" -> "www.one.one.one.one";
            default -> ip;
        }, 500);

        List<EstadisticasReportes.DominioUso> todos =
                EstadisticasReportes.dominios(List.of(primero, segundo), "PC-A", resolver, false);

        assertEquals(2, todos.size());
        assertEquals("dns.google", buscar(todos, "dns.google").dominio());
        assertEquals(2, buscar(todos, "dns.google").muestras());
        assertEquals(1, buscar(todos, "one.one.one.one").muestras());

        List<EstadisticasReportes.DominioUso> soloWeb =
                EstadisticasReportes.dominios(List.of(primero, segundo), "PC-A", resolver, true);

        assertEquals(1, soloWeb.size());
        assertEquals("dns.google", soloWeb.get(0).dominio());

        resolver.close();
    }

    @Test
    void clasificaDireccionesYFalsePuertosWeb() {
        assertTrue(EstadisticasReportes.esPrivada("192.168.0.1"));
        assertTrue(EstadisticasReportes.esPrivada("10.1.2.3"));
        assertTrue(EstadisticasReportes.esPrivada("172.16.0.1"));
        assertTrue(EstadisticasReportes.esPrivada("172.31.255.255"));
        assertTrue(EstadisticasReportes.esPrivada("127.0.0.1"));
        assertTrue(EstadisticasReportes.esPrivada("::1"));
        assertTrue(EstadisticasReportes.esPrivada("fe80::1"));
        assertTrue(EstadisticasReportes.esPrivada("fc00::1"));
        assertFalse(EstadisticasReportes.esPrivada("8.8.8.8"));
        assertFalse(EstadisticasReportes.esPrivada("1.1.1.1"));
        assertFalse(EstadisticasReportes.esPrivada("172.32.0.1"));
        assertFalse(EstadisticasReportes.esPrivada("2001:4860:4860::8888"));

        assertTrue(EstadisticasReportes.esPuertoWeb(443));
        assertTrue(EstadisticasReportes.esPuertoWeb(8080));
        assertFalse(EstadisticasReportes.esPuertoWeb(22));
    }

    private static EstadisticasReportes.DominioUso buscar(
            List<EstadisticasReportes.DominioUso> dominios, String nombre) {
        return dominios.stream()
                .filter(d -> d.dominio().equals(nombre))
                .findFirst()
                .orElseThrow();
    }

    private static EquipoReportado base(String host, LocalDateTime fecha) {
        EquipoReportado reporte = new EquipoReportado();
        reporte.setHostname(host);
        reporte.setFecha(fecha);
        reporte.setRamTotalMb(1000);
        reporte.setRamLibreMb(400);
        return reporte;
    }
}
