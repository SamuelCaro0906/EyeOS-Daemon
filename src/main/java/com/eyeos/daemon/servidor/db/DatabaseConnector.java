package com.eyeos.daemon.servidor.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnector {

    private final String url;
    private final String usuario;
    private final String clave;

    public DatabaseConnector(String url, String usuario, String clave) {
        this.url = url;
        this.usuario = usuario;
        this.clave = clave;
    }

    public static DatabaseConnector porDefecto() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnector.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            System.err.println("[EyeOS] No se pudo leer db.properties: " + e.getMessage());
        }
        return new DatabaseConnector(
                props.getProperty("db.url", "jdbc:postgresql://localhost:5432/eyeos"),
                props.getProperty("db.user", "postgres"),
                props.getProperty("db.password", "postgres")
        );
    }

    public Connection conectar() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }

    public boolean disponible() {
        try (Connection con = conectar()) {
            return con.isValid(3);
        } catch (SQLException e) {
            return false;
        }
    }

    public void asegurarColumnas() {
        String[] sentencias = {
                "ALTER TABLE reportes ADD COLUMN IF NOT EXISTS uptime_segundos BIGINT",
                "ALTER TABLE reportes ADD COLUMN IF NOT EXISTS foreground_app TEXT",
                "ALTER TABLE conexiones ADD COLUMN IF NOT EXISTS remota TEXT",
                "ALTER TABLE conexiones ADD COLUMN IF NOT EXISTS puerto_remoto INTEGER"
        };
        try (Connection con = conectar(); Statement st = con.createStatement()) {
            for (String sql : sentencias) {
                st.execute(sql);
            }
        } catch (SQLException e) {
            System.err.println("[EyeOS] No se pudieron migrar columnas: " + e.getMessage());
        }
    }

    public String getUrl() {
        return url;
    }

    public String getUsuario() {
        return usuario;
    }
}
