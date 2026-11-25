
package com.carmotors.modelo;

import java.time.LocalDate;

public record SolicitudServicioReporteDato(
        long ordenId,
        String tipoMantenimiento,
        LocalDate fechaSolicitud
) {
}
