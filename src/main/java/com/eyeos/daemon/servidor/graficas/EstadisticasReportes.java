package com.eyeos.daemon.servidor.graficas;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class EstadisticasReportes {

    public static final long UMBRAL_SESION_SEGUNDOS = 120;
    public static final long MAX_DELTA_SEGUNDOS = 120;
    public static final double DELTA_DEFECTO_MINUTOS = 0.42;
    private static final int MAX_PUNTOS_RAM = 120;
    private static final int MAX_SERIES = 8;
    private static final int MAX_ITEMS = 10;
    private static final Set<String> APPS_IGNORADAS = Set.of("idle", "lockapp", "logonui");

    private EstadisticasReportes() {
    }

    public record PuntoRam(LocalDateTime fecha, double pct, long usadaMb) {
    }

    public record Sesion(String hostname, LocalDateTime inicio, LocalDateTime fin, double minutos) {
    }

    public record UsoDia(LocalDate dia, double horas) {
    }

    public record AppUso(String app, double minutos) {
    }

    public record DominioUso(String dominio, long muestras, LocalDateTime ultima) {
    }

    public record Conteo(String etiqueta, long valor) {
    }

    public record Arranque(String hostname, LocalDateTime fecha) {
    }

    public record Cobertura(int total, int conActividad, int conArranque) {
    }

    public record ResumenRam(double promedioPct, double maximaPct, long ramTotalMb, int muestras) {
    }

    public record ResumenUso(double horasPorDia, double minutosPorSesion, int sesiones) {
    }

    public record ResumenInicio(int horaMasComun, double horaPromedio, long registros) {
    }

    public record ResumenApps(String top, double minutosTop, long distintas) {
    }

    public record ResumenDominios(String top, long muestrasTop, long distintos) {
    }

    public static String clave(EquipoReportado reporte) {
        String host = reporte.getHostname() != null && !reporte.getHostname().isBlank()
                ? reporte.getHostname() : "?";
        if (reporte.getFecha() == null) {
            return host + "|sin-fecha|" + System.identityHashCode(reporte);
        }
        return host + "|" + reporte.getFecha();
    }

    public static List<EquipoReportado> deduplicar(Collection<EquipoReportado> reportes) {
        if (reportes == null) {
            return List.of();
        }
        Map<String, EquipoReportado> unicos = new LinkedHashMap<>();
        for (EquipoReportado reporte : reportes) {
            if (reporte == null || reporte.getHostname() == null || reporte.getHostname().isBlank()) {
                continue;
            }
            unicos.putIfAbsent(clave(reporte), reporte);
        }
        List<EquipoReportado> lista = new ArrayList<>(unicos.values());
        lista.sort(Comparator.comparing(EquipoReportado::getFecha,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return lista;
    }

    public static List<String> hostnames(Collection<EquipoReportado> reportes) {
        return deduplicar(reportes).stream()
                .map(EquipoReportado::getHostname)
                .distinct()
                .sorted()
                .toList();
    }

    private static List<EquipoReportado> filtrar(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = deduplicar(reportes);
        if (hostname == null || hostname.isBlank() || "Todos".equalsIgnoreCase(hostname)) {
            return base;
        }
        return base.stream().filter(r -> hostname.equals(r.getHostname())).toList();
    }

    public static Cobertura cobertura(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        int conActividad = 0;
        int conArranque = 0;
        for (EquipoReportado reporte : base) {
            if (reporte.getForegroundApp() != null && !reporte.getForegroundApp().isBlank()) {
                conActividad++;
            }
            if (reporte.getUptimeSegundos() > 0) {
                conArranque++;
            }
        }
        return new Cobertura(base.size(), conActividad, conArranque);
    }

    private static Map<String, List<EquipoReportado>> agruparPorEquipo(List<EquipoReportado> reportes) {
        Map<String, List<EquipoReportado>> porEquipo = new LinkedHashMap<>();
        for (EquipoReportado reporte : reportes) {
            porEquipo.computeIfAbsent(reporte.getHostname(), k -> new ArrayList<>()).add(reporte);
        }
        for (List<EquipoReportado> lista : porEquipo.values()) {
            lista.sort(Comparator.comparing(EquipoReportado::getFecha,
                    Comparator.nullsLast(Comparator.naturalOrder())));
        }
        return porEquipo;
    }

    public static Map<String, List<PuntoRam>> seriesRam(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = deduplicar(reportes);
        Map<String, List<EquipoReportado>> porEquipo = agruparPorEquipo(base);
        Map<String, List<PuntoRam>> resultado = new LinkedHashMap<>();
        if (hostname != null && !hostname.isBlank() && !"Todos".equalsIgnoreCase(hostname)) {
            List<PuntoRam> serie = serieRam(porEquipo.getOrDefault(hostname, List.of()));
            if (!serie.isEmpty()) {
                resultado.put(hostname, serie);
            }
            return resultado;
        }
        porEquipo.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(MAX_SERIES)
                .forEach(e -> {
                    List<PuntoRam> serie = serieRam(e.getValue());
                    if (!serie.isEmpty()) {
                        resultado.put(e.getKey(), serie);
                    }
                });
        return resultado;
    }

    private static List<PuntoRam> serieRam(List<EquipoReportado> reportes) {
        List<PuntoRam> serie = new ArrayList<>();
        for (EquipoReportado reporte : reportes) {
            if (reporte.getFecha() == null || reporte.getRamTotalMb() <= 0) {
                continue;
            }
            long usada = Math.max(0, reporte.getRamTotalMb() - reporte.getRamLibreMb());
            double pct = Math.min(100.0, usada * 100.0 / reporte.getRamTotalMb());
            serie.add(new PuntoRam(reporte.getFecha(), redondear(pct), usada));
        }
        if (serie.size() > MAX_PUNTOS_RAM) {
            serie = new ArrayList<>(serie.subList(serie.size() - MAX_PUNTOS_RAM, serie.size()));
        }
        return serie;
    }

    public static ResumenRam resumenRam(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        double suma = 0;
        double maxima = 0;
        long totalSuma = 0;
        int muestras = 0;
        for (EquipoReportado reporte : base) {
            if (reporte.getRamTotalMb() <= 0) {
                continue;
            }
            long usada = Math.max(0, reporte.getRamTotalMb() - reporte.getRamLibreMb());
            double pct = Math.min(100.0, usada * 100.0 / reporte.getRamTotalMb());
            suma += pct;
            maxima = Math.max(maxima, pct);
            totalSuma += reporte.getRamTotalMb();
            muestras++;
        }
        return new ResumenRam(
                muestras == 0 ? 0 : redondear(suma / muestras),
                redondear(maxima),
                muestras == 0 ? 0 : totalSuma / muestras,
                muestras);
    }

    public static List<Sesion> sesiones(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        List<Sesion> crudas = new ArrayList<>();
        for (Map.Entry<String, List<EquipoReportado>> entry : agruparPorEquipo(base).entrySet()) {
            LocalDateTime inicio = null;
            LocalDateTime fin = null;
            for (EquipoReportado reporte : entry.getValue()) {
                if (reporte.getFecha() == null) {
                    continue;
                }
                if (inicio == null) {
                    inicio = reporte.getFecha();
                    fin = reporte.getFecha();
                    continue;
                }
                if (Duration.between(fin, reporte.getFecha()).toSeconds() > UMBRAL_SESION_SEGUNDOS) {
                    crudas.add(new Sesion(entry.getKey(), inicio, fin, 0));
                    inicio = reporte.getFecha();
                    fin = reporte.getFecha();
                } else {
                    fin = reporte.getFecha();
                }
            }
            if (inicio != null) {
                crudas.add(new Sesion(entry.getKey(), inicio, fin, 0));
            }
        }
        List<Sesion> resultado = new ArrayList<>();
        for (Sesion sesion : crudas) {
            double minutos = Duration.between(sesion.inicio(), sesion.fin()).toSeconds() / 60.0;
            if (minutos <= 0) {
                minutos = DELTA_DEFECTO_MINUTOS;
            }
            resultado.add(new Sesion(sesion.hostname(), sesion.inicio(), sesion.fin(), minutos));
        }
        resultado.sort(Comparator.comparing(Sesion::inicio));
        return resultado;
    }

    public static List<UsoDia> tiempoUsoPorDia(Collection<EquipoReportado> reportes, String hostname) {
        Map<LocalDate, Double> porDia = new TreeMap<>();
        for (Sesion sesion : sesiones(reportes, hostname)) {
            porDia.merge(sesion.inicio().toLocalDate(), sesion.minutos() / 60.0, Double::sum);
        }
        List<UsoDia> lista = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : porDia.entrySet()) {
            lista.add(new UsoDia(entry.getKey(), redondear2(entry.getValue())));
        }
        if (lista.size() > 30) {
            return new ArrayList<>(lista.subList(lista.size() - 30, lista.size()));
        }
        return lista;
    }

    public static ResumenUso resumenUso(Collection<EquipoReportado> reportes, String hostname) {
        List<Sesion> sesiones = sesiones(reportes, hostname);
        if (sesiones.isEmpty()) {
            return new ResumenUso(0, 0, 0);
        }
        double totalMinutos = 0;
        Set<LocalDate> dias = new HashSet<>();
        for (Sesion sesion : sesiones) {
            totalMinutos += sesion.minutos();
            dias.add(sesion.inicio().toLocalDate());
        }
        double promedioSesion = totalMinutos / sesiones.size();
        double porDia = dias.isEmpty() ? 0 : totalMinutos / dias.size() / 60.0;
        return new ResumenUso(redondear2(porDia), redondear(promedioSesion), sesiones.size());
    }

    public static List<Arranque> arranques(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        Map<String, Set<LocalDateTime>> vistos = new LinkedHashMap<>();
        for (EquipoReportado reporte : base) {
            if (reporte.getFecha() == null || reporte.getUptimeSegundos() <= 0) {
                continue;
            }
            LocalDateTime arranque = reporte.getFecha().minusSeconds(reporte.getUptimeSegundos())
                    .truncatedTo(ChronoUnit.MINUTES);
            vistos.computeIfAbsent(reporte.getHostname(), k -> new HashSet<>()).add(arranque);
        }
        List<Arranque> lista = new ArrayList<>();
        for (Map.Entry<String, Set<LocalDateTime>> entry : vistos.entrySet()) {
            for (LocalDateTime fecha : entry.getValue()) {
                lista.add(new Arranque(entry.getKey(), fecha));
            }
        }
        lista.sort(Comparator.comparing(Arranque::fecha));
        return lista;
    }

    public static List<Conteo> horasInicio(Collection<EquipoReportado> reportes, String hostname) {
        Map<Integer, Long> conteo = new TreeMap<>();
        for (Arranque arranque : arranques(reportes, hostname)) {
            conteo.merge(arranque.fecha().getHour(), 1L, Long::sum);
        }
        List<Conteo> lista = new ArrayList<>();
        for (int hora = 0; hora < 24; hora++) {
            lista.add(new Conteo(String.format("%02d:00", hora), conteo.getOrDefault(hora, 0L)));
        }
        return lista;
    }

    public static ResumenInicio resumenInicio(Collection<EquipoReportado> reportes, String hostname) {
        List<Conteo> horas = horasInicio(reportes, hostname);
        long total = 0;
        long maximo = 0;
        int horaMasComun = -1;
        double sumaPonderada = 0;
        for (Conteo conteo : horas) {
            total += conteo.valor();
            if (conteo.valor() > maximo) {
                maximo = conteo.valor();
                horaMasComun = Integer.parseInt(conteo.etiqueta().substring(0, 2));
            }
            sumaPonderada += Integer.parseInt(conteo.etiqueta().substring(0, 2)) * conteo.valor();
        }
        double promedio = total == 0 ? 0 : sumaPromedio(sumaPonderada, total);
        return new ResumenInicio(total == 0 ? -1 : horaMasComun, redondear(promedio), total);
    }

    private static double sumaPromedio(double suma, long total) {
        return suma / total;
    }

    public static List<AppUso> appsMasUsadas(Collection<EquipoReportado> reportes, String hostname) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        Map<String, Double> minutos = new HashMap<>();
        for (List<EquipoReportado> lista : agruparPorEquipo(base).values()) {
            for (int i = 0; i < lista.size(); i++) {
                EquipoReportado reporte = lista.get(i);
                if (reporte.getFecha() == null) {
                    continue;
                }
                String app = limpiarApp(reporte.getForegroundApp());
                if (app.isBlank()) {
                    app = procesoPrincipal(reporte);
                }
                if (app.isBlank()) {
                    continue;
                }
                double delta = DELTA_DEFECTO_MINUTOS;
                if (i + 1 < lista.size() && lista.get(i + 1).getFecha() != null) {
                    long segundos = Duration.between(reporte.getFecha(), lista.get(i + 1).getFecha()).toSeconds();
                    if (segundos > 0) {
                        delta = Math.min(segundos, MAX_DELTA_SEGUNDOS) / 60.0;
                    }
                }
                minutos.merge(app, delta, Double::sum);
            }
        }
        return topApps(minutos);
    }

    private static String procesoPrincipal(EquipoReportado reporte) {
        ProcesoInfo mejor = null;
        for (ProcesoInfo proceso : reporte.getProcesos()) {
            if (proceso == null || limpiarApp(proceso.getNombre()).isBlank()) {
                continue;
            }
            if (mejor == null
                    || proceso.getMemMb() > mejor.getMemMb()
                    || (proceso.getMemMb() == mejor.getMemMb() && proceso.getCpuPct() > mejor.getCpuPct())) {
                mejor = proceso;
            }
        }
        return mejor == null ? "" : limpiarApp(mejor.getNombre());
    }

    private static List<AppUso> topApps(Map<String, Double> minutos) {
        return minutos.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(MAX_ITEMS)
                .map(e -> new AppUso(e.getKey(), redondear(e.getValue())))
                .toList();
    }

    private static String limpiarApp(String nombre) {
        if (nombre == null) {
            return "";
        }
        String app = nombre.trim();
        if (app.toLowerCase().endsWith(".exe")) {
            app = app.substring(0, app.length() - 4);
        }
        if (app.isBlank() || APPS_IGNORADAS.contains(app.toLowerCase())) {
            return "";
        }
        return app;
    }

    public static ResumenApps resumenApps(Collection<EquipoReportado> reportes, String hostname) {
        List<AppUso> apps = appsMasUsadas(reportes, hostname);
        if (apps.isEmpty()) {
            return new ResumenApps("", 0, 0);
        }
        return new ResumenApps(apps.get(0).app(), apps.get(0).minutos(), apps.size());
    }

    public static List<DominioUso> dominios(Collection<EquipoReportado> reportes, String hostname,
                                            ResolverDominios resolver, boolean soloWeb) {
        List<EquipoReportado> base = filtrar(reportes, hostname);
        Set<String> ips = new HashSet<>();
        for (EquipoReportado reporte : base) {
            for (ConexionRed conexion : reporte.getConexiones()) {
                if (!"ESTABLISHED".equalsIgnoreCase(conexion.getEstado())) {
                    continue;
                }
                String ip = conexion.getDireccionRemota();
                if (ip == null || ip.isBlank() || esPrivada(ip)) {
                    continue;
                }
                if (soloWeb && !esPuertoWeb(conexion.getPuertoRemoto())) {
                    continue;
                }
                ips.add(ip);
            }
        }
        Map<String, String> resueltos = new HashMap<>();
        if (resolver != null) {
            resueltos.putAll(resolver.resolverTodos(ips));
        }
        for (String ip : ips) {
            resueltos.putIfAbsent(ip, ip);
        }
        Map<String, Long> conteo = new HashMap<>();
        Map<String, LocalDateTime> ultima = new HashMap<>();
        for (EquipoReportado reporte : base) {
            Set<String> delReporte = new HashSet<>();
            for (ConexionRed conexion : reporte.getConexiones()) {
                if (!"ESTABLISHED".equalsIgnoreCase(conexion.getEstado())) {
                    continue;
                }
                String ip = conexion.getDireccionRemota();
                if (ip == null || ip.isBlank() || esPrivada(ip)) {
                    continue;
                }
                if (soloWeb && !esPuertoWeb(conexion.getPuertoRemoto())) {
                    continue;
                }
                String dominio = resueltos.getOrDefault(ip, ip);
                if (dominio != null && !dominio.isBlank()) {
                    delReporte.add(dominio);
                }
            }
            for (String dominio : delReporte) {
                conteo.merge(dominio, 1L, Long::sum);
                if (reporte.getFecha() != null) {
                    ultima.merge(dominio, reporte.getFecha(),
                            (a, b) -> a.isAfter(b) ? a : b);
                }
            }
        }
        return conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(MAX_ITEMS)
                .map(e -> new DominioUso(e.getKey(), e.getValue(), ultima.get(e.getKey())))
                .toList();
    }

    public static ResumenDominios resumenDominios(List<DominioUso> dominios) {
        if (dominios.isEmpty()) {
            return new ResumenDominios("", 0, 0);
        }
        return new ResumenDominios(dominios.get(0).dominio(), dominios.get(0).muestras(), dominios.size());
    }

    static boolean esPuertoWeb(int puerto) {
        return puerto == 80 || puerto == 443 || puerto == 8080 || puerto == 8443;
    }

    static boolean esPrivada(String ip) {
        if (ip == null || ip.isBlank()) {
            return true;
        }
        String valor = ip.trim().toLowerCase();
        if (valor.startsWith("::ffff:")) {
            valor = valor.substring(7);
        }
        if (valor.equals("::1") || valor.equals("::") || valor.equals("0:0:0:0:0:0:0:1")) {
            return true;
        }
        if (valor.startsWith("fe80:") || valor.startsWith("fc") || valor.startsWith("fd")) {
            return true;
        }
        if (valor.startsWith("127.") || valor.startsWith("10.") || valor.startsWith("192.168.")
                || valor.startsWith("169.254.") || valor.startsWith("0.")) {
            return true;
        }
        if (valor.startsWith("172.")) {
            String[] partes = valor.split("\\.");
            if (partes.length >= 2) {
                try {
                    int segundo = Integer.parseInt(partes[1]);
                    return segundo >= 16 && segundo <= 31;
                } catch (NumberFormatException ignored) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private static double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
