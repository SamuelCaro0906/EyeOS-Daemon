package com.eyeos.daemon.cliente.ui;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

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
        } catch (AWTException e) {
            System.err.println("[EyeOS] No se pudo crear el icono de bandeja: " + e.getMessage());
        }
    }

    public void quitar() {
        if (icono != null) {
            SystemTray.getSystemTray().remove(icono);
            icono = null;
        }
    }

    private Image imagen() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(0, 120, 215));
        g.fillRoundRect(0, 0, 16, 16, 5, 5);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("E", 5, 12);
        g.dispose();
        return img;
    }
}
