package com.carmotors.servicio;

import com.carmotors.dao.EvaluacionProveedorDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.EvaluacionProveedor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reglas de negocio y transacciones de las evaluaciones de proveedores.
 * No contiene SQL ni interpreta entradas de consola.
 */
public class EvaluacionProveedorService {

    private final EvaluacionProveedorDAO dao;
    private final Supplier<Connection> conexiones;

    /** Constructor para pruebas o para inyectar un DAO existente. */
    public EvaluacionProveedorService(
            EvaluacionProveedorDAO dao,
            Supplier<Connection> conexiones) {

        this.dao = Objects.requireNonNull(dao, "El DAO es obligatorio");
        this.conexiones = Objects.requireNonNull(
                conexiones, "El proveedor de conexiones es obligatorio"
        );
    }

    /** Constructor que aprovecha el Factory Method ya existente. */
    public EvaluacionProveedorService(
            DAOFactory<EvaluacionProveedorDAO> fabrica,
            Supplier<Connection> conexiones) {

        this(Objects.requireNonNull(fabrica, "La fabrica es obligatoria")
                .obtenerDAO(), conexiones);
    }

    /**
     * Guarda una evaluación únicamente si el lote pertenece al proveedor.
     * El objeto con ID generado se devuelve solo después del commit.
     */
    public EvaluacionProveedor registrar(EvaluacionProveedor evaluacion) {
        if (evaluacion == null) {
            throw new ValidacionException("La evaluacion es obligatoria");
        }
        if (evaluacion.getId() != 0) {
            throw new ValidacionException(
                    "No se puede volver a registrar una evaluacion con ID asignado"
            );
        }

        return enTransaccion(conexion -> {
            if (!dao.lotePerteneceAProveedor(
                    conexion, evaluacion.getLoteId(), evaluacion.getProveedorId())) {
                throw new ValidacionException(
                        "El lote no existe o no pertenece al proveedor indicado"
                );
            }

            try {
                return dao.insertar(conexion, evaluacion);
            } catch (AccesoDatosException error) {
                // El FK compuesto protege incluso si la relación cambió
                // después de nuestra consulta previa.
                if (error.getCause() instanceof SQLException sql
                        && sql.getErrorCode() == 1452) {
                    ValidacionException validacion = new ValidacionException(
                            "El lote no existe o no pertenece al proveedor indicado"
                    );
                    validacion.initCause(error);
                    throw validacion;
                }
                throw error;
            }
        });
    }

    public Optional<EvaluacionProveedor> buscarPorId(int id) {
        validarId(id, "evaluacion");
        return leer(conexion -> dao.buscarPorId(conexion, id));
    }

    public List<EvaluacionProveedor> listarPorProveedor(int proveedorId) {
        validarId(proveedorId, "proveedor");
        return leer(conexion -> dao.listarPorProveedor(conexion, proveedorId));
    }

    /** OptionalDouble.empty() significa sin evaluaciones, nunca 0 inventado. */
    public OptionalDouble promedioCalidad(int proveedorId) {
        return listarPorProveedor(proveedorId).stream()
                .mapToInt(EvaluacionProveedor::getCalificacionCalidad)
                .average();
    }

    public OptionalDouble promedioPuntualidad(int proveedorId) {
        return listarPorProveedor(proveedorId).stream()
                .mapToInt(EvaluacionProveedor::getCalificacionPuntualidad)
                .average();
    }

    private static void validarId(int id, String entidad) {
        if (id <= 0) {
            throw new ValidacionException(
                    "El ID de " + entidad + " debe ser mayor que cero"
            );
        }
    }

    private <T> T leer(Function<Connection, T> accion) {
        try (Connection conexion = conexiones.get()) {
            return accion.apply(conexion);
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la conexion de lectura", e
            );
        }
    }

    private <T> T enTransaccion(Function<Connection, T> accion) {
        try (Connection conexion = conexiones.get()) {
            conexion.setAutoCommit(false);
            try {
                T resultado = accion.apply(conexion);
                conexion.commit();
                return resultado;
            } catch (SQLException | RuntimeException e) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }

                if (e instanceof SQLException sql) {
                    throw new AccesoDatosException(
                            "Error al confirmar la transaccion", sql
                    );
                }
                throw (RuntimeException) e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la transaccion", e
            );
        }
    }
}
