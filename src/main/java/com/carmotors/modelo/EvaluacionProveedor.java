package com.carmotors.modelo;

import java.time.LocalDate;
import java.util.Objects;

/** Evaluacion de calidad y puntualidad de un proveedor sobre un lote. */
public final class EvaluacionProveedor {

    private final int id;
    private final int proveedorId;
    private final int loteId;
    private final int calificacionCalidad;
    private final int calificacionPuntualidad;
    private final LocalDate fechaEvaluacion;
    private final String observaciones;

    /** Para evaluaciones nuevas: id = 0 hasta persistir. */
    public EvaluacionProveedor(
            int proveedorId,
            int loteId,
            int calificacionCalidad,
            int calificacionPuntualidad,
            LocalDate fechaEvaluacion,
            String observaciones) {
        this(0, proveedorId, loteId, calificacionCalidad,
                calificacionPuntualidad, fechaEvaluacion, observaciones);
    }

    private EvaluacionProveedor(
            int id,
            int proveedorId,
            int loteId,
            int calificacionCalidad,
            int calificacionPuntualidad,
            LocalDate fechaEvaluacion,
            String observaciones) {
        if (id < 0) {
            throw new IllegalArgumentException("El ID de la evaluacion no puede ser negativo");
        }
        if (proveedorId <= 0) {
            throw new IllegalArgumentException("El ID del proveedor debe ser mayor que cero");
        }
        if (loteId <= 0) {
            throw new IllegalArgumentException("El ID del lote debe ser mayor que cero");
        }
        validarCalificacion(calificacionCalidad, "calidad");
        validarCalificacion(calificacionPuntualidad, "puntualidad");

        this.fechaEvaluacion = Objects.requireNonNull(
                fechaEvaluacion, "La fecha de evaluacion es obligatoria"
        );
        this.id = id;
        this.proveedorId = proveedorId;
        this.loteId = loteId;
        this.calificacionCalidad = calificacionCalidad;
        this.calificacionPuntualidad = calificacionPuntualidad;
        this.observaciones = observaciones == null || observaciones.isBlank()
                ? null : observaciones.strip();
    }

    /** Para reconstruir una evaluacion previamente guardada en MySQL. */
    public static EvaluacionProveedor desdePersistencia(
            int id,
            int proveedorId,
            int loteId,
            int calificacionCalidad,
            int calificacionPuntualidad,
            LocalDate fechaEvaluacion,
            String observaciones) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "Una evaluacion persistida necesita un ID valido"
            );
        }
        return new EvaluacionProveedor(
                id, proveedorId, loteId, calificacionCalidad,
                calificacionPuntualidad, fechaEvaluacion, observaciones
        );
    }

    private static void validarCalificacion(int calificacion, String criterio) {
        if (calificacion < 1 || calificacion > 5) {
            throw new IllegalArgumentException(
                    "La calificacion de " + criterio + " debe estar entre 1 y 5"
            );
        }
    }

    public int getId() { return id; }
    public int getProveedorId() { return proveedorId; }
    public int getLoteId() { return loteId; }
    public int getCalificacionCalidad() { return calificacionCalidad; }
    public int getCalificacionPuntualidad() { return calificacionPuntualidad; }
    public LocalDate getFechaEvaluacion() { return fechaEvaluacion; }
    public String getObservaciones() { return observaciones; }
}
