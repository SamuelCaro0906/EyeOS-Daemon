package com.eyeos.daemon.cliente;

import com.eyeos.daemon.cliente.collect.FileCollector;
import com.eyeos.daemon.cliente.config.AgentConfig;
import com.eyeos.daemon.cliente.config.ConfigStore;
import com.eyeos.daemon.cliente.config.ConsentStore;
import com.eyeos.daemon.cliente.schedule.AuditLoop;
import com.eyeos.daemon.cliente.startup.AutostartManager;
import com.eyeos.daemon.cliente.ui.TrayManager;
import com.eyeos.daemon.common.report.SeccionReporte;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AgentApp extends Application {

    private final ConfigStore configStore = new ConfigStore();
    private final ConsentStore consentStore = new ConsentStore();
    private final Map<SeccionReporte, CheckBox> checksSeccion = new EnumMap<>(SeccionReporte.class);

    private AgentConfig config;
    private AuditLoop loop;
    private TrayManager tray;
    private Stage stage;

    private TextField ftpHost;
    private Spinner<Integer> ftpPort;
    private TextField ftpUser;
    private PasswordField ftpPassword;
    private TextField remoteDir;
    private Spinner<Integer> intervalo;
    private CheckBox terminarProcesos;
    private TextArea carpetas;
    private CheckBox autorizacion;
    private TextArea log;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        this.config = configStore.cargar();
        this.loop = new AuditLoop(config, consentStore);
        this.loop.agregarListener(mensaje ->
                Platform.runLater(() -> log(mensaje)));

        stage.setTitle("EyeOS Daemon - Agente de Auditoria");
        stage.setScene(new Scene(construirVista(), 660, 760));
        Platform.setImplicitExit(false);
        stage.setOnCloseRequest(evento -> {
            evento.consume();
            ocultar();
        });

        inicializarBandeja();
        cargarEnFormulario();

        if (config.configuracionCompleta() && consentStore.tieneConsentimiento(config)) {
            loop.iniciar();
            log("Servicio iniciado. Intervalo: " + config.getIntervaloSegundos() + "s");
            ocultar();
        } else {
            stage.show();
        }
    }

    private VBox construirVista() {
        Label titulo = new Label("EyeOS Daemon - Agente");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label subtitulo = new Label("El agente solo envia las secciones y carpetas que autorices.");
        subtitulo.setStyle("-fx-text-fill: #555;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Servidor FTP:"), ftpHost = new TextField());
        form.addRow(1, new Label("Puerto:"), ftpPort = new Spinner<>(1, 65535, 21));
        form.addRow(2, new Label("Usuario FTP:"), ftpUser = new TextField());
        form.addRow(3, new Label("Contrasena FTP:"), ftpPassword = new PasswordField());
        form.addRow(4, new Label("Carpeta remota:"), remoteDir = new TextField());
        form.addRow(5, new Label("Intervalo (seg):"), intervalo = new Spinner<>(5, 3600, 25));
        GridPane.setHgrow(ftpHost, Priority.ALWAYS);
        GridPane.setHgrow(ftpUser, Priority.ALWAYS);
        GridPane.setHgrow(ftpPassword, Priority.ALWAYS);
        GridPane.setHgrow(remoteDir, Priority.ALWAYS);

        FlowPane secciones = new FlowPane(16, 8);
        for (SeccionReporte seccion : SeccionReporte.values()) {
            CheckBox check = new CheckBox(seccion.getEtiqueta());
            check.setSelected(true);
            checksSeccion.put(seccion, check);
            secciones.getChildren().add(check);
        }

        terminarProcesos = new CheckBox("Terminar procesos que superen el umbral de memoria (avanzado)");

        Label etiquetaCarpetas = new Label("Carpetas a enviar (una por linea, opcional):");
        carpetas = new TextArea();
        carpetas.setPrefRowCount(3);
        Button agregarCarpeta = new Button("Agregar carpeta...");
        agregarCarpeta.setOnAction(e -> seleccionarCarpeta());

        autorizacion = new CheckBox(
                "Autorizo el envio de las secciones y carpetas seleccionadas al servidor FTP indicado, "
                        + "incluyendo el nombre de la aplicacion en primer plano (sin titulos de ventana) "
                        + "y las IP/puertos remotos de las conexiones.");
        autorizacion.setWrapText(true);

        Button guardar = new Button("Guardar y aprobar");
        guardar.setDefaultButton(true);
        guardar.setOnAction(e -> guardarYActivar());
        Button auditar = new Button("Auditar ahora");
        auditar.setOnAction(e -> auditarAhora());
        Button pausar = new Button("Pausar / Reanudar");
        pausar.setOnAction(e -> alternarPausa());
        Button salir = new Button("Salir");
        salir.setOnAction(e -> salir());
        HBox botones = new HBox(10, guardar, auditar, pausar, salir);

        log = new TextArea();
        log.setEditable(false);
        log.setPrefRowCount(6);
        VBox.setVgrow(log, Priority.ALWAYS);

        VBox raiz = new VBox(12, titulo, subtitulo, form,
                new Label("Secciones a reportar:"), secciones,
                terminarProcesos, etiquetaCarpetas, carpetas, agregarCarpeta,
                autorizacion, botones, new Label("Registro:"), log);
        raiz.setPadding(new Insets(16));

        ScrollPane scroll = new ScrollPane(raiz);
        scroll.setFitToWidth(true);
        VBox contenedor = new VBox(scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return contenedor;
    }

    private void seleccionarCarpeta() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta a enviar");
        File elegida = chooser.showDialog(stage);
        if (elegida != null) {
            String actual = carpetas.getText();
            if (!actual.isBlank()) {
                actual += "\n";
            }
            carpetas.setText(actual + elegida.getAbsolutePath());
        }
    }

    private void cargarEnFormulario() {
        ftpHost.setText(config.getFtpHost());
        ftpPort.getValueFactory().setValue(config.getFtpPort());
        ftpUser.setText(config.getFtpUser());
        ftpPassword.setText(config.getFtpPassword());
        remoteDir.setText(config.getRemoteDir());
        intervalo.getValueFactory().setValue(config.getIntervaloSegundos());
        terminarProcesos.setSelected(config.isTerminarProcesos());
        for (Map.Entry<SeccionReporte, CheckBox> entrada : checksSeccion.entrySet()) {
            entrada.getValue().setSelected(config.getReportOptions().incluye(entrada.getKey()));
        }
        carpetas.setText(String.join("\n", config.getReportOptions().getCarpetas()));
        autorizacion.setSelected(consentStore.tieneConsentimiento(config));
    }

    private void leerFormulario() {
        config.setFtpHost(ftpHost.getText().trim());
        config.setFtpPort(ftpPort.getValue());
        config.setFtpUser(ftpUser.getText().trim());
        config.setFtpPassword(ftpPassword.getText());
        config.setRemoteDir(remoteDir.getText().trim());
        config.setIntervaloSegundos(intervalo.getValue());
        config.setTerminarProcesos(terminarProcesos.isSelected());
        for (Map.Entry<SeccionReporte, CheckBox> entrada : checksSeccion.entrySet()) {
            config.getReportOptions().habilitarSeccion(entrada.getKey(), entrada.getValue().isSelected());
        }
        config.getReportOptions().getCarpetas().clear();
        for (String linea : carpetas.getText().split("\\R")) {
            String ruta = linea.trim();
            if (!ruta.isBlank()) {
                config.getReportOptions().getCarpetas().add(ruta);
            }
        }
    }

    private void guardarYActivar() {
        try {
            leerFormulario();
            if (config.getFtpHost().isBlank() || config.getFtpUser().isBlank()) {
                alerta("Debes indicar el servidor FTP y el usuario.");
                return;
            }
            if (!autorizacion.isSelected()) {
                alerta("Debes marcar la casilla de autorizacion para continuar.");
                return;
            }
            config.setHabilitada(true);
            configStore.guardar(config);

            List<Path> archivosDetectados = new FileCollector().recolectar(config.getReportOptions().getCarpetas());
            log("Carpetas configuradas: " + config.getReportOptions().getCarpetas().size()
                    + " (" + archivosDetectados.size() + " archivos por enviar).");
            if (config.getReportOptions().getCarpetas().isEmpty()) {
                log("Nota: si quieres enviar archivos, usa 'Agregar carpeta...' antes de guardar.");
            } else if (archivosDetectados.isEmpty()) {
                log("Aviso: las carpetas no contienen archivos validos (ocultos o mayores a 20 MB).");
            }

            if (config.isRecordarConsentimiento()) {
                String token = consentStore.registrar(config);
                log("Consentimiento registrado: " + token);
            } else {
                consentStore.revocar();
            }
            loop.iniciar();
            log("Configuracion guardada. Servicio activo cada " + config.getIntervaloSegundos() + "s.");
            if (config.isAutostart()) {
                boolean ok = new AutostartManager().instalar(comandoAutostart());
                log(ok ? "Autostart instalado." : "No se pudo instalar el autostart.");
            }
            ocultar();
        } catch (Exception e) {
            alerta("Error al guardar: " + e.getMessage());
        }
    }

    private void auditarAhora() {
        Thread hilo = new Thread(() -> {
            try {
                String resultado = loop.ejecutarCicloManual();
                Platform.runLater(() -> log(resultado));
            } catch (Exception e) {
                Platform.runLater(() -> log("ERROR: " + e.getMessage()));
            }
        }, "eyeos-audit-manual");
        hilo.setDaemon(true);
        hilo.start();
    }

    private void alternarPausa() {
        config.setHabilitada(!config.isHabilitada());
        try {
            configStore.guardar(config);
        } catch (Exception e) {
            log("No se pudo guardar el estado: " + e.getMessage());
        }
        if (config.isHabilitada()) {
            loop.iniciar();
            log("Servicio reanudado.");
        } else {
            loop.detener();
            log("Servicio pausado.");
        }
    }

    private String comandoAutostart() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            return "\"" + appPath + "\"";
        }
        Path javaw = Path.of(System.getProperty("java.home"), "bin", "javaw.exe");
        String cp = System.getProperty("java.class.path");
        return "\"" + javaw + "\" --enable-native-access=ALL-UNNAMED -cp \"" + cp
                + "\" com.eyeos.daemon.cliente.AgentLauncher";
    }

    private void inicializarBandeja() {
        tray = new TrayManager();
        if (tray.soportado()) {
            tray.instalar("EyeOS Daemon", this::mostrar, this::salir);
        }
    }

    private void mostrar() {
        Platform.runLater(() -> {
            stage.setIconified(false);
            stage.show();
            stage.toFront();
        });
    }

    private void ocultar() {
        if (tray != null && tray.soportado()) {
            stage.hide();
        } else {
            stage.setIconified(true);
        }
    }

    private void salir() {
        loop.detener();
        if (tray != null) {
            tray.quitar();
        }
        Platform.exit();
        System.exit(0);
    }

    private void log(String mensaje) {
        if (log != null) {
            log.appendText(mensaje + "\n");
        }
    }

    private void alerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING, mensaje);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public List<SeccionReporte> seccionesDisponibles() {
        return List.of(SeccionReporte.values());
    }
}
