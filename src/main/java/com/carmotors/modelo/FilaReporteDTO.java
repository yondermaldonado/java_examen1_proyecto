package com.carmotors.modelo;

import java.time.LocalDateTime;

public record FilaReporteDTO(
        String codigo,
        String nombre,
        long cantidad,
        LocalDateTime fechaRecepcion,
        LocalDateTime fechaCompletado,
        LocalDateTime fechaEntrega,
        String estado
) {

    // Constructor para los reportes anteriores.
    public FilaReporteDTO(
            String codigo,
            String nombre,
            long cantidad
    ) {
        this(codigo, nombre, cantidad,
                null, null, null, null);
    }

    public FilaReporteDTO {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("Código vacío");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Nombre vacío");
        }

        if (cantidad < 0) {
            throw new IllegalArgumentException("Cantidad negativa");
        }
    }
}