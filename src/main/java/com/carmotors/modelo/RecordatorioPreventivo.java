
package com.carmotors.modelo;

import java.time.LocalDate;
import java.util.Objects;

public class RecordatorioPreventivo {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String VENCIDO = "VENCIDO";

    private final int vehiculoId;
    private final int operacionId;
    private final String nombreOperacion;
    private final LocalDate fechaUltimaEntrega;
    private final int intervaloMeses;
    private final LocalDate fechaProxima;
    private final String estado;

    public RecordatorioPreventivo(
            int vehiculoId,
            int operacionId,
            String nombreOperacion,
            LocalDate fechaUltimaEntrega,
            int intervaloMeses,
            LocalDate fechaConsulta) {

        if (vehiculoId <= 0 || operacionId <= 0) {
            throw new IllegalArgumentException(
                    "Los IDs deben ser positivos");
        }

        Objects.requireNonNull(
                nombreOperacion, "La operacion es obligatoria");

        if (nombreOperacion.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de la operacion es obligatorio");
        }

        Objects.requireNonNull(
                fechaUltimaEntrega,
                "La fecha de entrega es obligatoria");

        Objects.requireNonNull(
                fechaConsulta,
                "La fecha de consulta es obligatoria");

        if (intervaloMeses <= 0) {
            throw new IllegalArgumentException(
                    "El intervalo debe ser positivo");
        }

        this.vehiculoId = vehiculoId;
        this.operacionId = operacionId;
        this.nombreOperacion = nombreOperacion.strip();
        this.fechaUltimaEntrega = fechaUltimaEntrega;
        this.intervaloMeses = intervaloMeses;

        this.fechaProxima =
                fechaUltimaEntrega.plusMonths(intervaloMeses);

        this.estado = fechaProxima.isBefore(fechaConsulta)
                ? VENCIDO
                : PENDIENTE;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public int getOperacionId() {
        return operacionId;
    }

    public String getNombreOperacion() {
        return nombreOperacion;
    }

    public LocalDate getFechaUltimaEntrega() {
        return fechaUltimaEntrega;
    }

    public int getIntervaloMeses() {
        return intervaloMeses;
    }

    public LocalDate getFechaProxima() {
        return fechaProxima;
    }

    public String getEstado() {
        return estado;
    }
}
