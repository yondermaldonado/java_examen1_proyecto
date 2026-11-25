
package com.carmotors.modelo;

import java.util.List;

public record ReporteDTO(
        TipoReporte tipo,
        String titulo,
        FiltroReporte filtro,
        List<FilaReporteDTO> filas
) {

    public ReporteDTO {

        if (tipo == null || filtro == null || filas == null) {
            throw new IllegalArgumentException(
                    "Los datos del reporte son obligatorios"
            );
        }

        if (titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException(
                    "El título del reporte es obligatorio"
            );
        }

        filas = List.copyOf(filas);
    }
}
