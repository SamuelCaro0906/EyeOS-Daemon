package com.eyeos.daemon.servidor;

import com.eyeos.daemon.cliente.config.AgentConfig;
import com.eyeos.daemon.cliente.config.ConfigStore;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.ui.Tema;
import com.eyeos.daemon.servidor.dao.ReporteDAO;
import com.eyeos.daemon.servidor.dao.ReporteDAOArrayImpl;
import com.eyeos.daemon.servidor.dao.ReporteDAOJdbcImpl;
import com.eyeos.daemon.servidor.db.DatabaseConnector;
import com.eyeos.daemon.servidor.graficas.EstadisticasReportes;
import com.eyeos.daemon.servidor.graficas.PanelEstadisticas;
import com.eyeos.daemon.servidor.service.ReporteService;
import com.eyeos.daemon.servidor.service.ReporteServiceImpl;
import com.eyeos.daemon.servidor.sync.CacheReportes;
import com.eyeos.daemon.servidor.sync.FtpSyncService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public class DashboardApp extends Application {

    private final FtpSyncService syncService = new FtpSyncService();
    private final CacheReportes cache = new CacheReportes();
    private final ObservableList<EquipoReportado> datos = FXCollections.observableArrayList();
    private final FilteredList<EquipoReportado> filtrados = new FilteredList<>(datos, p -> true);
    private final DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TextField ftpHost = new TextField();
    private final Spinner<Integer> ftpPort = new Spinner<>(1, 65535, 21);
    private final TextField ftpUser = new TextField();
    private final PasswordField ftpPassword = new PasswordField();
    private final TextField remoteDir = new TextField("/EyeOS");
    private final TextField busqueda = new TextField();
    private final Label estado = new Label("Listo.");
    private final TableView<EquipoReportado> tabla = new TableView<>();
    private final PanelEstadisticas panelEstadisticas = new PanelEstadisticas();
    private final Set<String> clavesGuardadas = new HashSet<>();

    /* Cabecera y KPIs */
    private final Label pildoraEstado = new Label("Sin conexion");
    private final Label contadorCabecera = new Label("0 equipos · 0 reportes");
    private final Label kpiEquipos = new Label("0");
    private final Label kpiReportes = new Label("0");
    private final Label kpiSincronizacion = new Label("Nunca");
    private final Label kpiBaseDatos = new Label("-");
    private final Button botonTema = new Button("Modo oscuro");

    /* Estado de carga */
    private final ProgressIndicator progreso = new ProgressIndicator();
    private Button botonSincronizar;

    private ReporteService servicio;
    private boolean usarCache;
    private boolean sincronizando;
    private String ultimoEstadoBd = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        DatabaseConnector db = DatabaseConnector.porDefecto();
        ReporteDAO dao;
        String estadoBd;
        if (db.disponible()) {
            db.asegurarColumnas();
            dao = new ReporteDAOJdbcImpl(db);
            estadoBd = "PostgreSQL conectado";
            ultimoEstadoBd = estadoBd;
        } else {
            dao = new ReporteDAOArrayImpl();
            usarCache = true;
            estadoBd = "PostgreSQL no disponible (modo memoria)";
            ultimoEstadoBd = estadoBd;
        }
        servicio = new ReporteServiceImpl(dao);

        stage.setTitle("EyeOS Daemon - Consola de Gestion");
        cargarConfiguracionAgente();

        Scene escena = new Scene(construirVista(), 1180, 800);
        Tema.aplicar(escena);
        stage.setScene(escena);
        aplicarEstadoBaseDatos(db.disponible());

        datos.addListener((ListChangeListener<EquipoReportado>) cambio -> {
            panelEstadisticas.actualizar(datos);
            actualizarKpis();
        });

        if (!(dao instanceof ReporteDAOArrayImpl)) {
            try {
                datos.setAll(servicio.listarReportes());
                for (EquipoReportado reporte : datos) {
                    clavesGuardadas.add(EstadisticasReportes.clave(reporte));
                }
            } catch (Exception e) {
                setEstado("No se pudieron cargar reportes: " + e.getMessage(), "estado-error");
            }
        } else {
            datos.setAll(cache.cargar());
            for (EquipoReportado reporte : datos) {
                clavesGuardadas.add(EstadisticasReportes.clave(reporte));
            }
            estadoBd += " | cache local: " + datos.size() + " reportes";
        }
        panelEstadisticas.actualizar(datos);
        actualizarKpis();
        setEstado(estadoBd + " | registros: " + datos.size(), usarCache ? "estado-info" : "estado-ok");
        stage.show();
    }

    @Override
    public void stop() {
        panelEstadisticas.cerrar();
    }

    private void cargarConfiguracionAgente() {
        try {
            AgentConfig cfg = new ConfigStore().cargar();
            if (cfg.getFtpHost() == null || cfg.getFtpHost().isBlank()) {
                return;
            }
            ftpHost.setText(cfg.getFtpHost());
            ftpPort.getValueFactory().setValue(cfg.getFtpPort());
            ftpUser.setText(cfg.getFtpUser());
            ftpPassword.setText(cfg.getFtpPassword());
            remoteDir.setText(cfg.getRemoteDir());
        } catch (Exception e) {
            System.err.println("[EyeOS] No se pudo precargar la configuracion del agente: " + e.getMessage());
        }
    }

    /* ============================================================
       Vistas
       ============================================================ */

    private BorderPane construirVista() {
        crearColumnas();

        VBox superior = new VBox(12, construirCabecera(), construirFilaKpis());
        superior.setPadding(new Insets(0));

        VBox reportes = new VBox(10, construirTarjetaFtp(), construirFiltros(),
                construirBarraBusqueda(), tabla);
        reportes.setPadding(new Insets(14, 22, 14, 22));
        VBox.setVgrow(tabla, Priority.ALWAYS);

        Tab tabReportes = new Tab("Reportes", reportes);
        Tab tabEstadisticas = new Tab("Estadisticas", panelEstadisticas);
        tabReportes.setClosable(false);
        tabEstadisticas.setClosable(false);
        TabPane tabs = new TabPane(tabReportes, tabEstadisticas);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane root = new BorderPane();
        root.setTop(superior);
        root.setCenter(tabs);
        root.setBottom(construirBarraEstado());
        return root;
    }

    private HBox construirCabecera() {
        Label marca = new Label("EyeOS Daemon");
        marca.getStyleClass().add("marca");
        Label subtitulo = new Label("Consola de gestion de equipos");
        subtitulo.getStyleClass().add("marca-sub");
        VBox titulos = new VBox(1, marca, subtitulo);

        pildoraEstado.getStyleClass().addAll("pildora", "pildora-error");
        contadorCabecera.getStyleClass().add("contador-cabecera");

        botonTema.getStyleClass().add("boton-tema");
        botonTema.setOnAction(e -> {
            boolean oscuro = Tema.alternar(escena());
            botonTema.setText(oscuro ? "Modo claro" : "Modo oscuro");
        });

        Region hueco = new Region();
        HBox.setHgrow(hueco, Priority.ALWAYS);

        HBox barra = new HBox(14, titulos, hueco, pildoraEstado, contadorCabecera, botonTema);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("cabecera");
        return barra;
    }

    private Scene escena() {
        return botonTema.getScene();
    }

    private HBox construirFilaKpis() {
        HBox fila = new HBox(12,
                tarjetaKpi("Equipos monitorados", kpiEquipos),
                tarjetaKpi("Reportes totales", kpiReportes),
                tarjetaKpi("Ultima sincronizacion", kpiSincronizacion),
                tarjetaKpi("Base de datos", kpiBaseDatos));
        fila.getStyleClass().add("fila-kpis");
        return fila;
    }

    private VBox tarjetaKpi(String etiqueta, Label valor) {
        Label rotulo = new Label(etiqueta);
        rotulo.getStyleClass().add("kpi-etiqueta");
        valor.getStyleClass().add("kpi-valor");
        VBox caja = new VBox(3, rotulo, valor);
        caja.getStyleClass().add("tarjeta-kpi");
        HBox.setHgrow(caja, Priority.ALWAYS);
        return caja;
    }

    private VBox construirTarjetaFtp() {
        GridPane conexion = new GridPane();
        conexion.setHgap(8);
        conexion.setVgap(8);
        conexion.add(new Label("Servidor FTP:"), 0, 0);
        conexion.add(ftpHost, 1, 0);
        conexion.add(new Label("Puerto:"), 2, 0);
        conexion.add(ftpPort, 3, 0);
        conexion.add(new Label("Usuario:"), 0, 1);
        conexion.add(ftpUser, 1, 1);
        conexion.add(new Label("Contrasena:"), 2, 1);
        conexion.add(ftpPassword, 3, 1);
        conexion.add(new Label("Carpeta remota:"), 0, 2);
        conexion.add(remoteDir, 1, 2);

        botonSincronizar = new Button("Sincronizar desde FTP");
        botonSincronizar.getStyleClass().add("boton-primario");
        botonSincronizar.setOnAction(e -> sincronizar());
        conexion.add(botonSincronizar, 2, 2, 2, 1);
        GridPane.setHgrow(ftpHost, Priority.ALWAYS);
        GridPane.setHgrow(ftpUser, Priority.ALWAYS);
        GridPane.setHgrow(ftpPassword, Priority.ALWAYS);
        GridPane.setHgrow(remoteDir, Priority.ALWAYS);

        Label titulo = new Label("Conexion y sincronizacion FTP");
        titulo.getStyleClass().add("titulo-seccion");

        VBox tarjeta = new VBox(10, titulo, conexion);
        tarjeta.getStyleClass().add("tarjeta");
        return tarjeta;
    }

    private FlowPane construirFiltros() {
        FlowPane filtros = new FlowPane(14, 8);
        for (TableColumn<EquipoReportado, ?> columna : tabla.getColumns()) {
            CheckBox check = new CheckBox(columna.getText());
            check.setSelected(true);
            columna.visibleProperty().bind(check.selectedProperty());
            filtros.getChildren().add(check);
        }
        filtros.getStyleClass().add("filtros");
        return filtros;
    }

    private HBox construirBarraBusqueda() {
        busqueda.setPromptText("Buscar equipo, usuario o sistema operativo...");
        busqueda.getStyleClass().add("campo-busqueda");
        busqueda.textProperty().addListener((obs, anterior, valor) ->
                filtrados.setPredicate(reporte -> coincide(reporte, valor)));

        Label rotulo = new Label("Filtrar:");
        Region hueco = new Region();
        HBox.setHgrow(busqueda, Priority.ALWAYS);
        HBox.setHgrow(hueco, Priority.NEVER);
        HBox barra = new HBox(8, rotulo, busqueda, hueco);
        barra.setAlignment(Pos.CENTER_LEFT);
        return barra;
    }

    private HBox construirBarraEstado() {
        progreso.setPrefSize(18, 18);
        progreso.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        progreso.setVisible(false);
        progreso.setManaged(false);

        estado.getStyleClass().add("estado-info");
        HBox barra = new HBox(10, progreso, estado);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.getStyleClass().add("barra-estado");
        return barra;
    }

    /* ============================================================
       Columnas y logica de tabla
       ============================================================ */

    private void crearColumnas() {
        tabla.getColumns().setAll(List.of(
                columna("Equipo", EquipoReportado::getHostname, 130),
                columna("Usuario", EquipoReportado::getUsuario, 110),
                columna("Fecha", r -> r.getFecha() != null ? r.getFecha().format(fmt) : "", 150),
                columna("Sistema Operativo", r -> texto(r.getSoNombre()) + " " + texto(r.getSoVersion()), 200),
                columna("RAM (total/libre MB)", r -> r.getRamTotalMb() + " / " + r.getRamLibreMb(), 150),
                columna("IP", r -> String.join(", ", r.getIps()), 160),
                columna("MAC", r -> String.join(", ", r.getMacs()), 160),
                columna("BIOS Serial", EquipoReportado::getBiosSerial, 150),
                columna("UUID Placa", EquipoReportado::getBaseboardUuid, 200)
        ));
        tabla.setPlaceholder(new Label("No hay reportes. Usa 'Sincronizar desde FTP' para cargar datos."));
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private TableColumn<EquipoReportado, String> columna(
            String titulo, Function<EquipoReportado, String> extractor, double ancho) {
        TableColumn<EquipoReportado, String> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(cd -> new ReadOnlyStringWrapper(extractor.apply(cd.getValue())));
        columna.setPrefWidth(ancho);
        return columna;
    }

    private boolean coincide(EquipoReportado reporte, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String t = texto.toLowerCase();
        return contiene(reporte.getHostname(), t) || contiene(reporte.getUsuario(), t)
                || contiene(reporte.getSoNombre(), t);
    }

    private boolean contiene(String valor, String texto) {
        return valor != null && valor.toLowerCase().contains(texto);
    }

    private String texto(String valor) {
        return valor != null ? valor : "";
    }

    /* ============================================================
       KPIs y estado
       ============================================================ */

    private void actualizarKpis() {
        long equipos = EstadisticasReportes.hostnames(datos).size();
        kpiEquipos.setText(String.valueOf(equipos));
        kpiReportes.setText(String.valueOf(datos.size()));
        contadorCabecera.setText(equipos + " equipos · " + datos.size() + " reportes");
    }

    private void aplicarEstadoBaseDatos(boolean disponible) {
        pildoraEstado.setText(disponible ? "PostgreSQL conectado" : "Modo memoria");
        pildoraEstado.getStyleClass().removeAll("pildora-ok", "pildora-error", "pildora-warn");
        pildoraEstado.getStyleClass().add(disponible ? "pildora-ok" : "pildora-warn");

        kpiBaseDatos.setText(disponible ? "Conectado" : "En memoria");
        kpiBaseDatos.getStyleClass().removeAll("kpi-valor-ok", "kpi-valor-error");
        kpiBaseDatos.getStyleClass().add(disponible ? "kpi-valor-ok" : "kpi-valor-error");
    }

    private void setEstado(String texto, String clase) {
        estado.setText(texto);
        estado.getStyleClass().removeAll("estado-info", "estado-ok", "estado-error", "estado-cargando");
        estado.getStyleClass().add(clase);
    }

    private void mostrarCargando(boolean cargando) {
        progreso.setVisible(cargando);
        progreso.setManaged(cargando);
        if (botonSincronizar != null) {
            botonSincronizar.setDisable(cargando);
        }
    }

    /* ============================================================
       Sincronizacion FTP
       ============================================================ */

    private void sincronizar() {
        if (sincronizando) {
            setEstado("Ya hay una sincronizacion en curso.", "estado-info");
            return;
        }
        String host = ftpHost.getText().trim();
        String usuario = ftpUser.getText().trim();
        String clave = ftpPassword.getText();
        String directorio = remoteDir.getText().trim();
        int puerto = ftpPort.getValue();

        if (host.isBlank() || usuario.isBlank()) {
            setEstado("Indica el servidor y el usuario FTP.", "estado-error");
            return;
        }
        sincronizando = true;
        mostrarCargando(true);
        setEstado("Sincronizando desde " + host + "...", "estado-cargando");

        List<EquipoReportado> existentes = List.copyOf(datos);
        Thread hilo = new Thread(() -> {
            try {
                List<EquipoReportado> reportes = syncService.sincronizar(host, puerto, usuario, clave, directorio);
                List<EquipoReportado> unicos = EstadisticasReportes.deduplicar(
                        java.util.stream.Stream.concat(existentes.stream(), reportes.stream()).toList());
                int guardados = 0;
                for (EquipoReportado reporte : unicos) {
                    String claveReporte = EstadisticasReportes.clave(reporte);
                    if (clavesGuardadas.contains(claveReporte)) {
                        continue;
                    }
                    try {
                        servicio.guardarReporte(reporte);
                        if (usarCache) {
                            cache.guardar(reporte);
                        }
                        clavesGuardadas.add(claveReporte);
                        guardados++;
                    } catch (Exception e) {
                        System.err.println("[EyeOS] No se pudo guardar " + reporte.getHostname() + ": " + e.getMessage());
                    }
                }
                int totalGuardados = guardados;
                Platform.runLater(() -> {
                    datos.setAll(unicos);
                    kpiSincronizacion.setText(horaActual());
                    setEstado("Sincronizados: " + unicos.size() + " (" + reportes.size()
                            + " leidos) | nuevos guardados: " + totalGuardados, "estado-ok");
                    sincronizando = false;
                    mostrarCargando(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setEstado("Error de sincronizacion: " + e.getMessage(), "estado-error");
                    sincronizando = false;
                    mostrarCargando(false);
                });
            }
        }, "eyeos-sync");
        hilo.setDaemon(true);
        hilo.start();
    }

    private String horaActual() {
        return java.time.LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
