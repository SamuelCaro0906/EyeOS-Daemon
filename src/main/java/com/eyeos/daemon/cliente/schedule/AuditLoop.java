package com.eyeos.daemon.cliente.schedule;

import com.eyeos.daemon.cliente.collect.FileCollector;
import com.eyeos.daemon.cliente.collect.ReportAssembler;
import com.eyeos.daemon.cliente.config.AgentConfig;
import com.eyeos.daemon.cliente.config.ConfigStore;
import com.eyeos.daemon.cliente.config.ConsentStore;
import com.eyeos.daemon.cliente.ftp.FtpUploader;
import com.eyeos.daemon.cliente.guard.ProcessGuard;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReporteParser;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.ReportWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class AuditLoop {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final AgentConfig config;
    private final ConsentStore consentStore;
    private final ReportAssembler assembler = new ReportAssembler();
    private final ReportWriter writer = new ReportWriter();
    private final FtpUploader uploader = new FtpUploader();
    private final FileCollector fileCollector = new FileCollector();
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    private ScheduledExecutorService executor;
    private Path scriptBat;

    public AuditLoop(AgentConfig config, ConsentStore consentStore) {
        this.config = config;
        this.consentStore = consentStore;
    }

    public void agregarListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void usarScript(Path scriptBat) {
        this.scriptBat = scriptBat;
    }

    public synchronized void iniciar() {
        detener();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread hilo = new Thread(r, "eyeos-audit");
            hilo.setDaemon(true);
            return hilo;
        });
        executor.scheduleWithFixedDelay(this::cicloSeguro, 1,
                config.getIntervaloSegundos(), TimeUnit.SECONDS);
    }

    public synchronized void detener() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public synchronized boolean estaActivo() {
        return executor != null && !executor.isShutdown();
    }

    public String ejecutarCicloManual() throws IOException {
        return ejecutarCiclo();
    }

    private void cicloSeguro() {
        try {
            ejecutarCiclo();
        } catch (Exception e) {
            notificar("ERROR: " + e.getMessage());
        }
    }

    private String ejecutarCiclo() throws IOException {
        if (!config.isHabilitada()) {
            return "Agente pausado.";
        }
        if (!consentStore.tieneConsentimiento(config)) {
            return "Sin consentimiento vigente para la configuracion actual.";
        }

        ReportOptions options = config.getReportOptions();
        EquipoReportado reporte;
        String contenido;
        if (scriptBat != null && Files.exists(scriptBat)) {
            Path salida = rutaSalida();
            ejecutarScript(salida);
            contenido = Files.readString(salida, StandardCharsets.UTF_8);
            reporte = new ReporteParser().parsear(contenido);
        } else {
            reporte = assembler.ensamblar(options, consentStore.tokenActual());
            contenido = writer.escribir(reporte, options.getSecciones());
        }

        ProcessGuard guard = new ProcessGuard(options.getUmbralProcesoMb(), config.isTerminarProcesos());
        for (String accion : guard.evaluar(reporte.getProcesos())) {
            notificar(accion);
        }

        String host = sane(reporte.getHostname());
        String nombre = "reporte_" + host + "_" + LocalDateTime.now().format(FMT) + ".txt";
        List<Path> archivos = fileCollector.recolectar(options.getCarpetas());

        uploader.subir(config, reporte.getHostname(), nombre, contenido, archivos);

        String detalle = options.getCarpetas().isEmpty()
                ? "0 archivos; sin carpetas configuradas"
                : archivos.size() + " archivos";
        String mensaje = "Enviado " + nombre + " (" + detalle + ").";
        notificar(mensaje);
        if (!options.getCarpetas().isEmpty() && archivos.isEmpty()) {
            notificar("Aviso: no se encontraron archivos en las carpetas configuradas.");
        }
        return mensaje;
    }

    private Path rutaSalida() throws IOException {
        Path carpeta = ConfigStore.carpetaAplicacion().resolve("reportes");
        Files.createDirectories(carpeta);
        return carpeta.resolve("reporte_" + LocalDateTime.now().format(FMT) + ".txt");
    }

    private void ejecutarScript(Path salida) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c",
                scriptBat.toString(), salida.toString());
        builder.redirectErrorStream(true);
        Process proceso = builder.start();
        boolean terminado;
        try {
            terminado = proceso.waitFor(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            proceso.destroyForcibly();
            throw new IOException("El script .bat fue interrumpido.", e);
        }
        String consola = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!terminado) {
            proceso.destroyForcibly();
            throw new IOException("El script .bat tardo demasiado.");
        }
        if (proceso.exitValue() != 0) {
            throw new IOException("El script .bat fallo: " + consola.trim());
        }
    }

    private void notificar(String mensaje) {
        for (Consumer<String> listener : listeners) {
            try {
                listener.accept(mensaje);
            } catch (Exception ignored) {
            }
        }
        System.out.println("[EyeOS] " + mensaje);
    }

    private String sane(String valor) {
        if (valor == null || valor.isBlank()) {
            return "equipo";
        }
        return valor.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
