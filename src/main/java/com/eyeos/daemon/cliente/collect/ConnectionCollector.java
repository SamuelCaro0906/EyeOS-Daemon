package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.SeccionReporte;
import oshi.software.os.InternetProtocolStats;
import oshi.software.os.OperatingSystem;

import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ConnectionCollector implements Collector {

    private static final int MAX_CONEXIONES = 100;

    private final OperatingSystem os;

    public ConnectionCollector(OperatingSystem os) {
        this.os = os;
    }

    @Override
    public SeccionReporte seccion() {
        return SeccionReporte.CONEXIONES;
    }

    @Override
    public void recolectar(EquipoReportado reporte, ReportOptions options) {
        InternetProtocolStats stats = os.getInternetProtocolStats();
        if (stats == null) {
            return;
        }
        List<InternetProtocolStats.IPConnection> conexiones = stats.getConnections();
        Set<String> vistos = new LinkedHashSet<>();

        for (InternetProtocolStats.IPConnection conexion : conexiones) {
            InternetProtocolStats.TcpState estado = conexion.getState();
            if (estado != InternetProtocolStats.TcpState.LISTEN
                    && estado != InternetProtocolStats.TcpState.ESTABLISHED) {
                continue;
            }
            String remota = "";
            int puertoRemoto = 0;
            if (estado == InternetProtocolStats.TcpState.ESTABLISHED) {
                remota = textoIp(conexion.getForeignAddress());
                puertoRemoto = conexion.getForeignPort();
                if (remota.isBlank() || "0.0.0.0".equals(remota) || "::".equals(remota)) {
                    remota = "";
                    puertoRemoto = 0;
                }
            }
            String clave = estado.name() + "|" + conexion.getType() + "|" + conexion.getLocalPort()
                    + "|" + remota + "|" + puertoRemoto;
            if (vistos.add(clave)) {
                reporte.getConexiones().add(new ConexionRed(
                        estado.name(), conexion.getType(), conexion.getLocalPort(),
                        remota, puertoRemoto));
            }
            if (reporte.getConexiones().size() >= MAX_CONEXIONES) {
                break;
            }
        }
    }

    static String textoIp(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }
}
