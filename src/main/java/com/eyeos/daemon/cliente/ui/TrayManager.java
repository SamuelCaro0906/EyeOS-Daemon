package com.eyeos.daemon.cliente.ui;

import com.eyeos.daemon.common.ui.Logo;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;

public class TrayManager {

    private final boolean soportado = SystemTray.isSupported();
    private TrayIcon icono;

    public boolean soportado() {
        return soportado;
    }

    public void instalar(String tooltip, Runnable abrir, Runnable salir) {
        if (!soportado || icono != null) {
            return;
        }
        try {
            PopupMenu menu = new PopupMenu();
            MenuItem abrirItem = new MenuItem("Abrir consola");
            abrirItem.addActionListener(e -> abrir.run());
            MenuItem salirItem = new MenuItem("Salir");
            salirItem.addActionListener(e -> salir.run());
            menu.add(abrirItem);
            menu.addSeparator();
            menu.add(salirItem);

            icono = new TrayIcon(imagen(), tooltip, menu);
            icono.setImageAutoSize(true);
            icono.addActionListener(e -> abrir.run());
            SystemTray.getSystemTray().add(icono);
        } catch (AWTException | RuntimeException e) {
            System.err.println("[EyeOS] No se pudo crear el icono de bandeja: " + e.getMessage());
        }
    }

    public void quitar() {
        if (icono != null) {
            SystemTray.getSystemTray().remove(icono);
            icono = null;
        }
    }

    /** Logo EyeOS cargado del classpath, en la variante segun el DPI de la pantalla. */
    private Image imagen() {
        return Logo.paraBandejaAwt();
    }
}
