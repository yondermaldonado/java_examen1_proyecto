package com.carmotors.controlador;

import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.EvaluacionProveedor;
import com.carmotors.servicio.EvaluacionProveedorService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Adapta las entradas de la vista y delega las reglas al Service.
 * No contiene SQL ni controla transacciones.
 */
public class EvaluacionProveedorController {

    private final EvaluacionProveedorService servicio;

    public EvaluacionProveedorController(EvaluacionProveedorService servicio) {
        this.servicio = Objects.requireNonNull(
                servicio, "El servicio de evaluaciones es obligatorio"
        );
    }

    /** Fecha en formato ISO: yyyy-MM-dd. */
    public EvaluacionProveedor registrarEvaluacion(
            int proveedorId,
            int loteId,
            String calidad,
            String puntualidad,
            String fechaEvaluacion,
            String observaciones) {

        EvaluacionProveedor evaluacion = new EvaluacionProveedor(
                proveedorId,
                loteId,
                convertirCalificacion(calidad, "calidad"),
                convertirCalificacion(puntualidad, "puntualidad"),
                convertirFecha(fechaEvaluacion),
                observaciones
        );

        return servicio.registrar(evaluacion);
    }

    public Optional<EvaluacionProveedor> buscarEvaluacionPorId(int id) {
        return servicio.buscarPorId(id);
    }

    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        return servicio.listarPorProveedor(proveedorId);
    }

    public OptionalDouble promedioCalidad(int proveedorId) {
        return servicio.promedioCalidad(proveedorId);
    }

    public OptionalDouble promedioPuntualidad(int proveedorId) {
        return servicio.promedioPuntualidad(proveedorId);
    }

    private static int convertirCalificacion(String texto, String criterio) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException(
                    "La calificacion de " + criterio + " es obligatoria"
            );
        }

        try {
            return Integer.parseInt(texto.strip());
        } catch (NumberFormatException e) {
            ValidacionException error = new ValidacionException(
                    "La calificacion de " + criterio + " debe ser un entero de 1 a 5"
            );
            error.initCause(e);
            throw error;
        }
    }

    private static LocalDate convertirFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("La fecha de evaluacion es obligatoria");
        }

        try {
            return LocalDate.parse(texto.strip());
        } catch (DateTimeParseException e) {
            ValidacionException error = new ValidacionException(
                    "La fecha debe tener formato yyyy-MM-dd, por ejemplo 2026-11-18"
            );
            error.initCause(e);
            throw error;
        }
    }
}
