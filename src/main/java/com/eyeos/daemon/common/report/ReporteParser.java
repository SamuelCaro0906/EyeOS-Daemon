package com.eyeos.daemon.common.report;

import com.eyeos.daemon.common.model.ConexionRed;
import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.model.ProcesoInfo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ReporteParser {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public EquipoReportado parsear(String contenido) {
        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException("El contenido del reporte esta vacio.");
        }
        EquipoReportado reporte = new EquipoReportado();
        String seccion = "";
        boolean inicioEncontrado = false;

        for (String lineaCruda : contenido.split("\n")) {
            String linea = lineaCruda.replace("\r", "").trim();
            if (linea.isEmpty()) {
                continue;
            }
            if (linea.equals(ReportWriter.INICIO)) {
                inicioEncontrado = true;
                continue;
            }
            if (linea.equals(ReportWriter.FIN)) {
                break;
            }
            if (linea.startsWith("[") && linea.endsWith("]")) {
                seccion = linea.substring(1, linea.length() - 1);
                continue;
            }
            if (linea.contains("=")) {
                int idx = linea.indexOf('=');
                aplicar(reporte, seccion, linea.substring(0, idx).trim(), linea.substring(idx + 1));
            } else if (seccion.equals("PROCESSES")) {
                if (!linea.startsWith("PID")) {
                    agregarProceso(reporte, linea);
                }
            } else if (seccion.equals("CONNECTIONS")) {
                if (!linea.startsWith("STATE")) {
                    agregarConexion(reporte, linea);
                }
            }
        }

        if (!inicioEncontrado) {
            throw new IllegalArgumentException("El texto no es un reporte EyeOS valido.");
        }
        if (reporte.getHostname() == null || reporte.getHostname().isBlank()) {
            throw new IllegalArgumentException("El reporte no contiene HOSTNAME.");
        }
        return reporte;
    }

    private void aplicar(EquipoReportado reporte, String seccion, String clave, String valor) {
        switch (clave) {
            case "TIMESTAMP" -> {
                try {
                    reporte.setFecha(LocalDateTime.parse(valor, FMT));
                } catch (Exception ignored) {
                }
            }
            case "HOSTNAME" -> reporte.setHostname(valor);
            case "USER" -> reporte.setUsuario(valor);
            case "CONSENT" -> reporte.setConsentimiento(valor);
            case "NAME" -> {
                if (seccion.equals("OS")) {
                    reporte.setSoNombre(valor);
                }
            }
            case "VERSION" -> {
                if (seccion.equals("OS")) {
                    reporte.setSoVersion(valor);
                }
            }
            case "EDITION" -> {
                if (seccion.equals("OS")) {
                    reporte.setSoEdicion(valor);
                }
            }
            case "TOTAL_MB" -> reporte.setRamTotalMb(aLong(valor));
            case "FREE_MB" -> reporte.setRamLibreMb(aLong(valor));
            case "IP" -> reporte.getIps().add(valor);
            case "MAC" -> reporte.getMacs().add(valor);
            case "BIOS_SERIAL" -> reporte.setBiosSerial(valor);
            case "BASEBOARD_UUID" -> reporte.setBaseboardUuid(valor);
            case "UPTIME_SECONDS" -> reporte.setUptimeSegundos(aLong(valor));
            case "FOREGROUND" -> reporte.setForegroundApp(valor);
            case "FOREGROUND_PID" -> reporte.setForegroundPid(aInt(valor));
            default -> {
            }
        }
    }

    private void agregarProceso(EquipoReportado reporte, String linea) {
        String[] partes = linea.split("\\|", -1);
        if (partes.length >= 4) {
            ProcesoInfo info = new ProcesoInfo();
            info.setPid(aInt(partes[0]));
            info.setNombre(partes[1]);
            info.setCpuPct(aDouble(partes[2]));
            info.setMemMb(aLong(partes[3]));
            reporte.getProcesos().add(info);
        }
    }

    private void agregarConexion(EquipoReportado reporte, String linea) {
        String[] partes = linea.split("\\|", -1);
        if (partes.length >= 3) {
            ConexionRed conexion = new ConexionRed();
            conexion.setEstado(partes[0]);
            conexion.setProtocolo(partes[1]);
            conexion.setPuerto(aInt(partes[2]));
            if (partes.length >= 5) {
                conexion.setDireccionRemota(partes[3]);
                conexion.setPuertoRemoto(aInt(partes[4]));
            }
            reporte.getConexiones().add(conexion);
        }
    }

    private int aInt(String valor) {
        try {
            return Integer.parseInt(valor.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private long aLong(String valor) {
        try {
            return Long.parseLong(valor.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private double aDouble(String valor) {
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return 0d;
        }
    }
}
