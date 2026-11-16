package com.eyeos.daemon.servidor.dao;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;
import com.eyeos.daemon.servidor.db.DatabaseConnector;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAOJdbcImpl implements ReporteDAO {

    private final DatabaseConnector db;

    public ReporteDAOJdbcImpl(DatabaseConnector db) {
        this.db = db;
    }

    @Override
    public void guardar(EquipoReportado reporte) {
        try (Connection con = db.conectar()) {
            con.setAutoCommit(false);
            try {
                int equipoId = upsertEquipo(con, reporte);
                int reporteId = insertarReporte(con, equipoId, reporte);
                insertarRed(con, reporteId, reporte);
                insertarConexiones(con, reporteId, reporte);
                insertarProcesos(con, reporteId, reporte);
                con.commit();
                reporte.setId(reporteId);
            } catch (SQLException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo guardar el reporte: " + e.getMessage(), e);
        }
    }

    private int upsertEquipo(Connection con, EquipoReportado r) throws SQLException {
        String sql = "INSERT INTO equipos (hostname, usuario, so_nombre, so_version, so_edicion, bios_serial, baseboard_uuid) "
                + "VALUES (?,?,?,?,?,?,?) "
                + "ON CONFLICT (hostname) DO UPDATE SET usuario = EXCLUDED.usuario, "
                + "so_nombre = EXCLUDED.so_nombre, so_version = EXCLUDED.so_version, "
                + "so_edicion = EXCLUDED.so_edicion, bios_serial = EXCLUDED.bios_serial, "
                + "baseboard_uuid = EXCLUDED.baseboard_uuid RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, r.getHostname());
            ps.setString(2, r.getUsuario());
            ps.setString(3, r.getSoNombre());
            ps.setString(4, r.getSoVersion());
            ps.setString(5, r.getSoEdicion());
            ps.setString(6, r.getBiosSerial());
            ps.setString(7, r.getBaseboardUuid());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private int insertarReporte(Connection con, int equipoId, EquipoReportado r) throws SQLException {
        String sql = "INSERT INTO reportes (equipo_id, fecha, consentimiento, ram_total_mb, ram_libre_mb, "
                + "uptime_segundos, foreground_app) VALUES (?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, equipoId);
            LocalDateTime fecha = r.getFecha() != null ? r.getFecha() : LocalDateTime.now();
            ps.setTimestamp(2, Timestamp.valueOf(fecha));
            ps.setString(3, r.getConsentimiento());
            ps.setLong(4, r.getRamTotalMb());
            ps.setLong(5, r.getRamLibreMb());
            ps.setLong(6, r.getUptimeSegundos());
            ps.setString(7, r.getForegroundApp());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void insertarRed(Connection con, int reporteId, EquipoReportado r) throws SQLException {
        String sql = "INSERT INTO red (reporte_id, tipo, valor) VALUES (?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (String ip : r.getIps()) {
                ps.setInt(1, reporteId);
                ps.setString(2, "IP");
                ps.setString(3, ip);
                ps.addBatch();
            }
            for (String mac : r.getMacs()) {
                ps.setInt(1, reporteId);
                ps.setString(2, "MAC");
                ps.setString(3, mac);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarConexiones(Connection con, int reporteId, EquipoReportado r) throws SQLException {
        String sql = "INSERT INTO conexiones (reporte_id, estado, protocolo, puerto, remota, puerto_remoto) "
                + "VALUES (?,?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ConexionRed conexion : r.getConexiones()) {
                ps.setInt(1, reporteId);
                ps.setString(2, conexion.getEstado());
                ps.setString(3, conexion.getProtocolo());
                ps.setInt(4, conexion.getPuerto());
                ps.setString(5, conexion.getDireccionRemota());
                ps.setInt(6, conexion.getPuertoRemoto());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void insertarProcesos(Connection con, int reporteId, EquipoReportado r) throws SQLException {
        String sql = "INSERT INTO procesos (reporte_id, pid, nombre, cpu_pct, mem_mb) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (ProcesoInfo proceso : r.getProcesos()) {
                ps.setInt(1, reporteId);
                ps.setInt(2, proceso.getPid());
                ps.setString(3, proceso.getNombre());
                ps.setDouble(4, proceso.getCpuPct());
                ps.setLong(5, proceso.getMemMb());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public List<EquipoReportado> listarTodos() {
        return consultar("", new Object[0]);
    }

    @Override
    public EquipoReportado buscarPorId(Integer id) {
        List<EquipoReportado> lista = consultar(" WHERE r.id = ?", new Object[]{id});
        return lista.isEmpty() ? null : lista.get(0);
    }

    @Override
    public List<EquipoReportado> listarPorEquipo(String hostname) {
        return consultar(" WHERE e.hostname = ?", new Object[]{hostname});
    }

    @Override
    public void actualizar(EquipoReportado reporte) {
        String sql = "UPDATE reportes SET fecha = ?, consentimiento = ?, ram_total_mb = ?, ram_libre_mb = ?, "
                + "uptime_segundos = ?, foreground_app = ? WHERE id = ?";
        try (Connection con = db.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            LocalDateTime fecha = reporte.getFecha() != null ? reporte.getFecha() : LocalDateTime.now();
            ps.setTimestamp(1, Timestamp.valueOf(fecha));
            ps.setString(2, reporte.getConsentimiento());
            ps.setLong(3, reporte.getRamTotalMb());
            ps.setLong(4, reporte.getRamLibreMb());
            ps.setLong(5, reporte.getUptimeSegundos());
            ps.setString(6, reporte.getForegroundApp());
            ps.setInt(7, reporte.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo actualizar el reporte: " + e.getMessage(), e);
        }
    }

    @Override
    public void eliminar(Integer id) {
        try (Connection con = db.conectar();
             PreparedStatement ps = con.prepareStatement("DELETE FROM reportes WHERE id = ?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("No se pudo eliminar el reporte: " + e.getMessage(), e);
        }
    }

    private List<EquipoReportado> consultar(String filtro, Object[] params) {
        String sql = "SELECT r.id, r.fecha, r.consentimiento, r.ram_total_mb, r.ram_libre_mb, "
                + "r.uptime_segundos, r.foreground_app, "
                + "e.hostname, e.usuario, e.so_nombre, e.so_version, e.so_edicion, e.bios_serial, e.baseboard_uuid "
                + "FROM reportes r JOIN equipos e ON e.id = r.equipo_id" + filtro + " ORDER BY r.fecha DESC";
        List<EquipoReportado> resultado = new ArrayList<>();
        try (Connection con = db.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
            for (EquipoReportado reporte : resultado) {
                cargarRed(con, reporte);
                cargarConexiones(con, reporte);
                cargarProcesos(con, reporte);
            }
        } catch (SQLException e) {
            throw new RuntimeException("No se pudieron leer los reportes: " + e.getMessage(), e);
        }
        return resultado;
    }

    private EquipoReportado mapear(ResultSet rs) throws SQLException {
        EquipoReportado r = new EquipoReportado();
        r.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("fecha");
        if (ts != null) {
            r.setFecha(ts.toLocalDateTime());
        }
        r.setConsentimiento(rs.getString("consentimiento"));
        r.setRamTotalMb(rs.getLong("ram_total_mb"));
        r.setRamLibreMb(rs.getLong("ram_libre_mb"));
        r.setUptimeSegundos(rs.getLong("uptime_segundos"));
        r.setForegroundApp(rs.getString("foreground_app"));
        r.setHostname(rs.getString("hostname"));
        r.setUsuario(rs.getString("usuario"));
        r.setSoNombre(rs.getString("so_nombre"));
        r.setSoVersion(rs.getString("so_version"));
        r.setSoEdicion(rs.getString("so_edicion"));
        r.setBiosSerial(rs.getString("bios_serial"));
        r.setBaseboardUuid(rs.getString("baseboard_uuid"));
        return r;
    }

    private void cargarRed(Connection con, EquipoReportado reporte) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement("SELECT tipo, valor FROM red WHERE reporte_id = ?")) {
            ps.setInt(1, reporte.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if ("MAC".equals(rs.getString("tipo"))) {
                        reporte.getMacs().add(rs.getString("valor"));
                    } else {
                        reporte.getIps().add(rs.getString("valor"));
                    }
                }
            }
        }
    }

    private void cargarConexiones(Connection con, EquipoReportado reporte) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT estado, protocolo, puerto, remota, puerto_remoto FROM conexiones WHERE reporte_id = ?")) {
            ps.setInt(1, reporte.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reporte.getConexiones().add(new ConexionRed(
                            rs.getString("estado"), rs.getString("protocolo"), rs.getInt("puerto"),
                            rs.getString("remota"), rs.getInt("puerto_remoto")));
                }
            }
        }
    }

    private void cargarProcesos(Connection con, EquipoReportado reporte) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT pid, nombre, cpu_pct, mem_mb FROM procesos WHERE reporte_id = ? ORDER BY mem_mb DESC")) {
            ps.setInt(1, reporte.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reporte.getProcesos().add(new ProcesoInfo(
                            rs.getInt("pid"), rs.getString("nombre"),
                            rs.getDouble("cpu_pct"), rs.getLong("mem_mb")));
                }
            }
        }
    }
}
