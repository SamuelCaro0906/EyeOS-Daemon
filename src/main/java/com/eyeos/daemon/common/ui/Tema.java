package com.eyeos.daemon.common.ui;

import javafx.scene.Scene;

/**
 * Carga y alterna el tema visual de la aplicacion (claro/oscuro).
 * Los hojas de estilos viven en el classpath: /tema-claro.css y
 * /tema-oscuro.css, de modo que funcionan igual en desarrollo (mvn
 * javafx:run) y en las app-images generadas con jpackage.
 */
public final class Tema {

    public static final String CLARO = "/tema-claro.css";
    public static final String OSCURO = "/tema-oscuro.css";

    private static boolean oscuro;

    private Tema() {
    }

    /** Aplica el tema actual (o el indicado) a la escena dada. */
    public static void aplicar(Scene escena) {
        if (escena == null) {
            return;
        }
        escena.getStylesheets().setAll(ruta(oscuro));
    }

    /** Cambia al tema opuesto y lo aplica. Devuelve true si quedo oscuro. */
    public static boolean alternar(Scene escena) {
        oscuro = !oscuro;
        aplicar(escena);
        return oscuro;
    }

    public static boolean estaOscuro() {
        return oscuro;
    }

    private static String ruta(boolean modoOscuro) {
        String recurso = modoOscuro ? OSCURO : CLARO;
        java.net.URL url = Tema.class.getResource(recurso);
        if (url == null) {
            throw new IllegalStateException("No se encontro el tema en el classpath: " + recurso);
        }
        return url.toExternalForm();
    }
}
