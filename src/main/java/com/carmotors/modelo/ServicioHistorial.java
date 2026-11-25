
package com.carmotors.modelo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class ServicioHistorial {

    private final int servicioId;
    private final int vehiculoId;
    private final LocalDateTime fechaRecepcion;
    private final LocalDateTime fechaEntrega;
    private final String estado;
    private final String tipoMantenimiento;
    private final String descripcionProblema;
    private final List<OperacionHistorial> operaciones;

    // CONSTRUCTOR
    public ServicioHistorial(
            int servicioId,
            int vehiculoId,
            LocalDateTime fechaRecepcion,
            LocalDateTime fechaEntrega,
            String estado,
            String tipoMantenimiento,
            String descripcionProblema,
            List<OperacionHistorial> operaciones) {

        if (servicioId <= 0 || vehiculoId <= 0) {
            throw new IllegalArgumentException(
                    "Los IDs deben ser positivos");
        }

        Objects.requireNonNull(fechaRecepcion,
                "La fecha de recepcion es obligatoria");

        Objects.requireNonNull(estado,
                "El estado es obligatorio");

        Objects.requireNonNull(tipoMantenimiento,
                "El tipo de mantenimiento es obligatorio");

        Objects.requireNonNull(operaciones,
                "La lista de operaciones es obligatoria");

        if (estado.isBlank() || tipoMantenimiento.isBlank()) {
            throw new IllegalArgumentException(
                    "Estado y tipo no pueden estar vacios");
        }

        if (fechaEntrega != null
                && fechaEntrega.isBefore(fechaRecepcion)) {
            throw new IllegalArgumentException(
                    "La entrega no puede ser anterior a la recepcion");
        }

        this.servicioId = servicioId;
        this.vehiculoId = vehiculoId;
        this.fechaRecepcion = fechaRecepcion;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado.strip();
        this.tipoMantenimiento = tipoMantenimiento.strip();
        this.descripcionProblema = descripcionProblema;

        this.operaciones = List.copyOf(operaciones);
    }

    // GETTERS
    public int getServicioId() {
        return servicioId;
    }

    public int getVehiculoId() {
        return vehiculoId;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public String getEstado() {
        return estado;
    }

    public String getTipoMantenimiento() {
        return tipoMantenimiento;
    }

    public String getDescripcionProblema() {
        return descripcionProblema;
    }

    public List<OperacionHistorial> getOperaciones() {
        return operaciones;
    }

    // METODO DE CONSULTA
    public boolean fueEntregado() {
        return fechaEntrega != null;
    }
}
