package com.eyeos.daemon.servidor;

import com.eyeos.daemon.cliente.config.AgentConfig;
import com.eyeos.daemon.cliente.config.ConfigStore;
import com.eyeos.daemon.common.model.EquipoReportado;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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

    private ReporteService servicio;
    private boolean usarCache;
    private boolean sincronizando;

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
        } else {
            dao = new ReporteDAOArrayImpl();
            usarCache = true;
            estadoBd = "PostgreSQL no disponible (modo memoria)";
        }
        servicio = new ReporteServiceImpl(dao);

        stage.setTitle("EyeOS Daemon - Consola de Gestion");
        cargarConfiguracionAgente();
        stage.setScene(new Scene(construirVista(), 1180, 760));

        datos.addListener((ListChangeListener<EquipoReportado>) cambio ->
                panelEstadisticas.actualizar(datos));

        if (!(dao instanceof ReporteDAOArrayImpl)) {
            try {
                datos.setAll(servicio.listarReportes());
                for (EquipoReportado reporte : datos) {
                    clavesGuardadas.add(EstadisticasReportes.clave(reporte));
                }
            } catch (Exception e) {
                estado.setText("No se pudieron cargar reportes: " + e.getMessage());
            }
        } else {
            datos.setAll(cache.cargar());
            for (EquipoReportado reporte : datos) {
                clavesGuardadas.add(EstadisticasReportes.clave(reporte));
            }
            estadoBd += " | cache local: " + datos.size() + " reportes";
        }
        panelEstadisticas.actualizar(datos);
        estado.setText(estadoBd + " | registros: " + datos.size());
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

    private BorderPane construirVista() {
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

        Button sincronizar = new Button("Sincronizar desde FTP");
        sincronizar.setOnAction(e -> sincronizar());
        conexion.add(sincronizar, 2, 2, 2, 1);
        GridPane.setHgrow(ftpHost, Priority.ALWAYS);
        GridPane.setHgrow(ftpUser, Priority.ALWAYS);
        GridPane.setHgrow(ftpPassword, Priority.ALWAYS);
        GridPane.setHgrow(remoteDir, Priority.ALWAYS);

        crearColumnas();

        FlowPane filtros = new FlowPane(14, 8);
        for (TableColumn<EquipoReportado, ?> columna : tabla.getColumns()) {
            CheckBox check = new CheckBox(columna.getText());
            check.setSelected(true);
            columna.visibleProperty().bind(check.selectedProperty());
            filtros.getChildren().add(check);
        }

        HBox barraBusqueda = new HBox(8, new Label("Filtrar equipo/usuario:"), busqueda);
        HBox.setHgrow(busqueda, Priority.ALWAYS);
        busqueda.textProperty().addListener((obs, anterior, valor) ->
                filtrados.setPredicate(reporte -> coincide(reporte, valor)));

        tabla.setItems(filtrados);
        tabla.setPlaceholder(new Label("No hay reportes. Usa 'Sincronizar desde FTP'."));

        estado.setPadding(new Insets(8));

        VBox superior = new VBox(10, conexion);
        superior.setPadding(new Insets(12, 12, 0, 12));

        VBox reportes = new VBox(10, filtros, barraBusqueda, tabla);
        reportes.setPadding(new Insets(12));
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
        root.setBottom(estado);
        return root;
    }

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

    private void sincronizar() {
        if (sincronizando) {
            estado.setText("Ya hay una sincronizacion en curso.");
            return;
        }
        String host = ftpHost.getText().trim();
        String usuario = ftpUser.getText().trim();
        String clave = ftpPassword.getText();
        String directorio = remoteDir.getText().trim();
        int puerto = ftpPort.getValue();

        if (host.isBlank() || usuario.isBlank()) {
            estado.setText("Indica el servidor y el usuario FTP.");
            return;
        }
        sincronizando = true;
        estado.setText("Sincronizando desde " + host + "...");

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
                    estado.setText("Sincronizados: " + unicos.size() + " (" + reportes.size()
                            + " leidos) | nuevos guardados: " + totalGuardados);
                    sincronizando = false;
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    estado.setText("Error de sincronizacion: " + e.getMessage());
                    sincronizando = false;
                });
            }
        }, "eyeos-sync");
        hilo.setDaemon(true);
        hilo.start();
    }
}
