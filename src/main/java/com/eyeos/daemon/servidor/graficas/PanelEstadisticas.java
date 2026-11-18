package com.eyeos.daemon.servidor.graficas;

import com.eyeos.daemon.common.model.EquipoReportado;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PanelEstadisticas extends BorderPane {

    private final ComboBox<String> equipos = new ComboBox<>();
    private final CheckBox soloWeb = new CheckBox("Solo puertos web");
    private final Label estado = new Label("Sin datos");
    private final ResolverDominios resolver = new ResolverDominios();
    private final AtomicInteger generacion = new AtomicInteger();

    private final AreaChart<String, Number> ramChart;
    private final BarChart<String, Number> usoChart;
    private final BarChart<Number, String> appsChart;
    private final BarChart<Number, String> dominiosChart;
    private final BarChart<String, Number> horasChart;

    private final Label ramKpi = new Label();
    private final Label usoKpi = new Label();
    private final Label appsKpi = new Label();
    private final Label dominiosKpi = new Label();
    private final Label horasKpi = new Label();

    private final VBox contenido = new VBox(12);
    private List<EquipoReportado> datos = List.of();
    private boolean actualizando;

    public PanelEstadisticas() {
        setPadding(new Insets(14, 22, 14, 22));
        getStyleClass().add("panel-stats");

        CategoryAxis ramX = new CategoryAxis();
        ramX.setTickLabelRotation(-30);
        ramX.setTickLabelGap(4);
        NumberAxis ramY = new NumberAxis(0, 100, 10);
        ramY.setLabel("% usado");
        ramChart = new AreaChart<>(ramX, ramY);
        ramChart.setLegendVisible(true);

        NumberAxis usoY = new NumberAxis();
        usoY.setLabel("Horas");
        usoChart = new BarChart<>(new CategoryAxis(), usoY);

        NumberAxis appsX = new NumberAxis();
        appsX.setLabel("Minutos");
        appsChart = new BarChart<>(appsX, new CategoryAxis());

        NumberAxis dominiosX = new NumberAxis();
        dominiosX.setLabel("Reportes");
        dominiosChart = new BarChart<>(dominiosX, new CategoryAxis());

        NumberAxis horasY = new NumberAxis();
        horasY.setLabel("Arranques");
        horasChart = new BarChart<>(new CategoryAxis(), horasY);

        configurar(ramChart, 260);
        configurar(usoChart, 260);
        configurar(appsChart, 280);
        configurar(dominiosChart, 280);
        configurar(horasChart, 260);
        appsChart.setLegendVisible(false);
        dominiosChart.setLegendVisible(false);
        usoChart.setLegendVisible(false);
        horasChart.setLegendVisible(false);

        equipos.setPrefWidth(220);
        equipos.setOnAction(e -> {
            if (!actualizando) {
                refrescar();
            }
        });
        soloWeb.setOnAction(e -> refrescarDominios());
        Button exportar = new Button("Exportar panel PNG");
        exportar.getStyleClass().add("boton-primario");
        exportar.setOnAction(e -> exportar(contenido, "estadisticas"));
        estado.getStyleClass().add("estado-panel");
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox barra = new HBox(8, new Label("Equipo:"), equipos, soloWeb, espaciador, exportar, estado);
        barra.setAlignment(Pos.CENTER_LEFT);

        Label titulo = new Label("Estadisticas de uso");
        titulo.getStyleClass().add("titulo-panel");
        Label subtitulo = new Label(
                "Datos agregados de los reportes sincronizados. Actividad y arranque solo aparecen en reportes nuevos; los duplicados se descartan.");
        subtitulo.getStyleClass().add("subtitulo-panel");
        subtitulo.setWrapText(true);
        VBox encabezado = new VBox(6, titulo, subtitulo, barra);
        encabezado.setPadding(new Insets(0, 0, 10, 0));
        setTop(encabezado);

        contenido.setPadding(new Insets(4, 0, 12, 0));
        contenido.getChildren().addAll(
                tarjeta("Consumo de RAM", ramChart, ramKpi),
                tarjeta("Tiempo de uso por día", usoChart, usoKpi),
                tarjeta("Aplicaciones / procesos más usados", appsChart, appsKpi),
                tarjeta("Dominios más contactados", dominiosChart, dominiosKpi),
                tarjeta("Horas de arranque del equipo", horasChart, horasKpi));

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("stats-scroll");
        setCenter(scroll);
    }

    private void configurar(XYChart<?, ?> grafico, double altura) {
        grafico.setAnimated(false);
        grafico.setLegendVisible(false);
        grafico.setPrefHeight(altura);
        grafico.setMinHeight(altura);
    }

    private VBox tarjeta(String titulo, Node grafico, Label kpi) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("titulo-tarjeta");
        Button png = new Button("PNG");
        png.setOnAction(e -> exportar(grafico, titulo));
        Region espaciador = new Region();
        HBox.setHgrow(espaciador, Priority.ALWAYS);
        HBox cabecera = new HBox(8, etiqueta, espaciador, png);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        kpi.getStyleClass().add("kpi-texto");
        kpi.setWrapText(true);
        VBox caja = new VBox(6, cabecera, kpi, grafico);
        caja.getStyleClass().add("tarjeta");
        caja.setEffect(new DropShadow(8, 0, 2, Color.rgb(0, 0, 0, 0.10)));
        return caja;
    }

    public void actualizar(List<EquipoReportado> reportes) {
        datos = EstadisticasReportes.deduplicar(reportes);
        List<String> hosts = EstadisticasReportes.hostnames(datos);
        String seleccion = equipos.getValue();
        List<String> items = new ArrayList<>();
        items.add("Todos");
        items.addAll(hosts);
        actualizando = true;
        try {
            equipos.setItems(FXCollections.observableArrayList(items));
            equipos.setValue(items.contains(seleccion) ? seleccion : "Todos");
        } finally {
            actualizando = false;
        }
        refrescar();
    }

    private void refrescar() {
        if (equipos.getValue() == null) {
            return;
        }
        EstadisticasReportes.Cobertura cobertura =
                EstadisticasReportes.cobertura(datos, equipos.getValue());
        estado.setText(datos.size() + " reportes · " + EstadisticasReportes.hostnames(datos).size()
                + " equipos · actividad " + cobertura.conActividad() + "/" + cobertura.total()
                + " · arranque " + cobertura.conArranque() + "/" + cobertura.total());
        refrescarRam();
        refrescarUso();
        refrescarApps();
        refrescarHoras();
        refrescarDominios();
    }

    private void refrescarRam() {
        String equipo = equipos.getValue();
        Map<String, List<EstadisticasReportes.PuntoRam>> series =
                EstadisticasReportes.seriesRam(datos, equipo);
        ramChart.getData().clear();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM HH:mm");
        for (Map.Entry<String, List<EstadisticasReportes.PuntoRam>> entry : series.entrySet()) {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(entry.getKey());
            for (EstadisticasReportes.PuntoRam punto : entry.getValue()) {
                serie.getData().add(new XYChart.Data<>(punto.fecha().format(formato), punto.pct()));
            }
            ramChart.getData().add(serie);
        }
        EstadisticasReportes.ResumenRam resumen = EstadisticasReportes.resumenRam(datos, equipo);
        ramKpi.setText(textoRam(resumen));
    }

    static String textoRam(EstadisticasReportes.ResumenRam resumen) {
        if (resumen.muestras() == 0) {
            return "Sin datos de RAM";
        }
        return String.format(Locale.US,
                "Promedio %.1f%% · Maximo %.1f%% · RAM total %d MB · %d muestras",
                resumen.promedioPct(), resumen.maximaPct(), resumen.ramTotalMb(), resumen.muestras());
    }

    private void refrescarUso() {
        String equipo = equipos.getValue();
        usoChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");
        for (EstadisticasReportes.UsoDia dia : EstadisticasReportes.tiempoUsoPorDia(datos, equipo)) {
            serie.getData().add(new XYChart.Data<>(dia.dia().format(formato), dia.horas()));
        }
        usoChart.getData().add(serie);
        EstadisticasReportes.ResumenUso resumen = EstadisticasReportes.resumenUso(datos, equipo);
        double totalMinutos = EstadisticasReportes.sesiones(datos, equipo).stream()
                .mapToDouble(EstadisticasReportes.Sesion::minutos).sum();
        if (resumen.sesiones() == 0) {
            usoKpi.setText("Sin sesiones: se necesitan al menos 2 reportes del mismo equipo.");
        } else if (totalMinutos < 60) {
            usoKpi.setText(String.format(Locale.US,
                    "Total %.0f min · Sesion promedio %.0f min · %d sesiones",
                    totalMinutos, resumen.minutosPorSesion(), resumen.sesiones()));
        } else {
            usoKpi.setText(String.format(Locale.US,
                    "Promedio %.2f h/dia · Sesion promedio %.0f min · %d sesiones",
                    resumen.horasPorDia(), resumen.minutosPorSesion(), resumen.sesiones()));
        }
    }

    private void refrescarApps() {
        String equipo = equipos.getValue();
        List<EstadisticasReportes.AppUso> apps = EstadisticasReportes.appsMasUsadas(datos, equipo);
        appsChart.getData().clear();
        XYChart.Series<Number, String> serie = new XYChart.Series<>();
        for (int i = apps.size() - 1; i >= 0; i--) {
            EstadisticasReportes.AppUso app = apps.get(i);
            serie.getData().add(new XYChart.Data<>(app.minutos(), app.app()));
        }
        appsChart.getData().add(serie);
        EstadisticasReportes.ResumenApps resumen = EstadisticasReportes.resumenApps(datos, equipo);
        EstadisticasReportes.Cobertura cobertura = EstadisticasReportes.cobertura(datos, equipo);
        if (resumen.distintas() == 0) {
            appsKpi.setText("Sin datos: activa 'Actividad' o 'Procesos' en el agente y vuelve a sincronizar.");
        } else if (cobertura.conActividad() < cobertura.total()) {
            appsKpi.setText(String.format(Locale.US,
                    "Top: %s (%.0f min) · %d apps · actividad en %d/%d reportes (resto estimado por procesos)",
                    resumen.top(), resumen.minutosTop(), resumen.distintas(),
                    cobertura.conActividad(), cobertura.total()));
        } else {
            appsKpi.setText(String.format(Locale.US,
                    "Top: %s (%.0f min) · %d apps · actividad en %d/%d reportes",
                    resumen.top(), resumen.minutosTop(), resumen.distintas(),
                    cobertura.conActividad(), cobertura.total()));
        }
    }

    private void refrescarHoras() {
        String equipo = equipos.getValue();
        horasChart.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        for (EstadisticasReportes.Conteo conteo : EstadisticasReportes.horasInicio(datos, equipo)) {
            serie.getData().add(new XYChart.Data<>(conteo.etiqueta(), conteo.valor()));
        }
        horasChart.getData().add(serie);
        EstadisticasReportes.ResumenInicio resumen = EstadisticasReportes.resumenInicio(datos, equipo);
        if (resumen.registros() == 0) {
            horasKpi.setText("Sin arranques: los reportes deben incluir UPTIME_SECONDS (agente actualizado).");
        } else {
            List<EstadisticasReportes.Arranque> arranques = EstadisticasReportes.arranques(datos, equipo);
            String ultimo = arranques.get(arranques.size() - 1).fecha()
                    .format(DateTimeFormatter.ofPattern("dd/MM HH:mm"));
            horasKpi.setText(String.format(Locale.US,
                    "Hora mas comun %02d:00 · promedio %.1f h · %d %s · ultimo %s",
                    resumen.horaMasComun(), resumen.horaPromedio(), resumen.registros(),
                    resumen.registros() == 1 ? "arranque" : "arranques", ultimo));
        }
    }

    private void refrescarDominios() {
        String equipo = equipos.getValue();
        boolean soloWebActivo = soloWeb.isSelected();
        int gen = generacion.incrementAndGet();
        dominiosChart.getData().clear();
        dominiosKpi.setText("Resolviendo dominios...");
        List<EquipoReportado> base = datos;
        Thread hilo = new Thread(() -> {
            List<EstadisticasReportes.DominioUso> lista =
                    EstadisticasReportes.dominios(base, equipo, resolver, soloWebActivo);
            EstadisticasReportes.ResumenDominios resumen =
                    EstadisticasReportes.resumenDominios(lista);
            Platform.runLater(() -> {
                if (gen != generacion.get()) {
                    return;
                }
                dominiosChart.getData().clear();
                XYChart.Series<Number, String> serie = new XYChart.Series<>();
                for (int i = lista.size() - 1; i >= 0; i--) {
                    EstadisticasReportes.DominioUso dominio = lista.get(i);
                    serie.getData().add(new XYChart.Data<>(dominio.muestras(), dominio.dominio()));
                }
                dominiosChart.getData().add(serie);
                if (lista.isEmpty()) {
                    dominiosKpi.setText("Sin conexiones remotas registradas");
                } else {
                    boolean soloIps = lista.stream()
                            .allMatch(d -> d.dominio().matches("\\d{1,3}(\\.\\d{1,3}){3}"));
                    String nota = soloIps ? " (sin DNS inverso; se muestran IPs)" : "";
                    dominiosKpi.setText(String.format(Locale.US,
                            "Mas contactado: %s (%d reportes) · %d destinos%s",
                            resumen.top(), resumen.muestrasTop(), resumen.distintos(), nota));
                }
            });
        }, "eyeos-stats-dominios");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void exportar(Node nodo, String nombre) {
        try {
            FileChooser selector = new FileChooser();
            selector.setTitle("Guardar imagen");
            String limpio = nombre.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
            selector.setInitialFileName(limpio + ".png");
            selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imagen PNG", "*.png"));
            File archivo = selector.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (archivo == null) {
                return;
            }
            nodo.applyCss();
            if (nodo instanceof javafx.scene.Parent padre) {
                padre.layout();
            }
            SnapshotParameters parametros = new SnapshotParameters();
            parametros.setFill(Color.WHITE);
            WritableImage imagen = nodo.snapshot(parametros, null);
            int ancho = (int) imagen.getWidth();
            int alto = (int) imagen.getHeight();
            int[] pixeles = new int[ancho * alto];
            imagen.getPixelReader().getPixels(0, 0, ancho, alto,
                    PixelFormat.getIntArgbInstance(), pixeles, 0, ancho);
            BufferedImage buffered = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            buffered.setRGB(0, 0, ancho, alto, pixeles, 0, ancho);
            ImageIO.write(buffered, "png", archivo);
            estado.setText("PNG guardado: " + archivo.getName());
        } catch (Exception e) {
            estado.setText("No se pudo exportar: " + e.getMessage());
        }
    }

    public void cerrar() {
        generacion.incrementAndGet();
        resolver.close();
    }
}
