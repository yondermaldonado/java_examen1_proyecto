
package com.carmotors.modelo;

import java.time.LocalDate;

public record FiltroReporte(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        OrdenReporte orden
) {

    public FiltroReporte {

        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException(
                    "Las fechas son obligatorias"
            );
        }

        if (fechaInicio.isAfter(fechaFin)) {
            throw new IllegalArgumentException(
                    "La fecha inicial no puede ser posterior a la final"
            );
        }

        if (orden == null) {
            throw new IllegalArgumentException(
                    "Debes seleccionar un orden"
            );
        }
    }

    public boolean incluye(LocalDate fecha) {

        return fecha != null
                && !fecha.isBefore(fechaInicio)
                && !fecha.isAfter(fechaFin);
    }
}
