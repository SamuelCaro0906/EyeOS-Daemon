package com.eyeos.daemon.common.ui;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.InputStream;
import java.util.List;

/**
 * Recursos y tamanos del logo EyeOS.
 *
 * Centraliza rutas y medidas para que la cabecera, la ventana y la
 * bandeja del sistema usen siempre los mismos assets empaquetados en
 * el classpath (funciona igual en desarrollo y en las app-images de
 * jpackage). Para cambiar el tamano del logo basta con tocar una
 * constante de esta clase.
 */
public final class Logo {

    /* Tamanos de interfaz (px) */
    public static final int SM = 32;
    public static final int MD = 48;
    public static final int LG = 96;
    public static final int XL = 160;

    /* Rutas en el classpath */
    public static final String RUTA = "/imagenes/logo.png";
    public static final String RUTA_2X = "/imagenes/logo@2x.png";
    public static final String RUTA_ICONO_16 = "/imagenes/icono-simple-16.png";
    public static final String RUTA_ICONO_24 = "/imagenes/icono-simple-24.png";
    public static final String RUTA_ICONO_32 = "/imagenes/icono-32.png";
    public static final String RUTA_ICONO_48 = "/imagenes/icono-48.png";
    public static final String RUTA_ICONO_256 = "/imagenes/icono-256.png";

    /** Resolucion minima (dpi) a partir de la cual la bandeja usa el icono de 32px. */
    private static final double DPI_ALTO = 144;

    private Logo() {
    }

    /** Vista del logo escalada al tamanos dado, sin deformar (contain). */
    public static ImageView crear(int tamano) {
        String ruta = tamano > MD ? RUTA_2X : RUTA;
        ImageView vista = new ImageView(cargar(ruta));
        vista.setFitWidth(tamano);
        vista.setFitHeight(tamano);
        vista.setPreserveRatio(true);
        vista.setSmooth(true);
        return vista;
    }

    /** Imagenes para Stage.getIcons() (barra de titulo y barra de tareas). */
    public static List<Image> paraVentana() {
        return List.of(
                cargar(RUTA_ICONO_16),
                cargar(RUTA_ICONO_32),
                cargar(RUTA_ICONO_48),
                cargar(RUTA_ICONO_256));
    }

    /**
     * Icono AWT para la bandeja del sistema (AWT TrayIcon no acepta
     * imagenes de JavaFX). Usa la variante segun el DPI de la pantalla.
     */
    public static java.awt.Image paraBandejaAwt() {
        try (InputStream entrada = Logo.class.getResourceAsStream(rutaBandeja())) {
            if (entrada == null) {
                throw new IllegalStateException("No se encontro el logo en el classpath: " + rutaBandeja());
            }
            return javax.imageio.ImageIO.read(entrada);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer el icono de bandeja", e);
        }
    }

    private static String rutaBandeja() {
        return java.awt.Toolkit.getDefaultToolkit().getScreenResolution() >= DPI_ALTO
                ? RUTA_ICONO_32
                : RUTA_ICONO_16;
    }

    private static Image cargar(String ruta) {
        InputStream entrada = Logo.class.getResourceAsStream(ruta);
        if (entrada == null) {
            throw new IllegalStateException("No se encontro el logo en el classpath: " + ruta);
        }
        try (entrada) {
            return new Image(entrada);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("No se pudo leer el logo: " + ruta, e);
        }
    }
}
