package com.eyeos.daemon.cliente.collect;

import com.eyeos.daemon.common.model.EquipoReportado;
import com.eyeos.daemon.common.report.ReportOptions;
import com.eyeos.daemon.common.report.ReportWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportAssemblerSmokeTest {

    @Test
    void recolectaTelemetriaRealDeLaMaquina() {
        ReportOptions options = new ReportOptions();
        EquipoReportado reporte = new ReportAssembler().ensamblar(options, null);

        assertNotNull(reporte.getHostname());
        assertTrue(reporte.getRamTotalMb() > 0, "Deberia leer la RAM total via OSHI");
        assertTrue(reporte.getIps().size() >= 1, "Deberia leer al menos una IP");
        assertTrue(reporte.getUptimeSegundos() > 0, "Deberia leer el uptime via OSHI");

        String texto = new ReportWriter().escribir(reporte, options.getSecciones());
        assertTrue(texto.contains("UPTIME_SECONDS="));
        assertTrue(texto.contains("[ACTIVITY]"));
    }
}
