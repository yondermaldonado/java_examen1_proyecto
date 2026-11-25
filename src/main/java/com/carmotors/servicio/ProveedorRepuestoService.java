
package com.carmotors.servicio;

import com.carmotors.dao.ProveedorRepuestoDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ProveedorRepuesto;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProveedorRepuestoService {

    private final ProveedorRepuestoDAO dao;
    private final ProveedorConsulta proveedores;
    private final RepuestoConsulta repuestos;
    private final Supplier<Connection> conexiones;

    // =========================================
    // CONSTRUCTORES
    // =========================================

    /**
     * Constructor con DAO inyectado.
     * Especialmente útil durante las pruebas.
     */
    public ProveedorRepuestoService(
            ProveedorRepuestoDAO dao,
            ProveedorConsulta proveedores,
            RepuestoConsulta repuestos,
            Supplier<Connection> conexiones) {

        this.dao = Objects.requireNonNull(
                dao,
                "El DAO no puede ser nulo"
        );

        this.proveedores = Objects.requireNonNull(
                proveedores,
                "La consulta de proveedores es obligatoria"
        );

        this.repuestos = Objects.requireNonNull(
                repuestos,
                "La consulta de repuestos es obligatoria"
        );

        this.conexiones = Objects.requireNonNull(
                conexiones,
                "El proveedor de conexiones es obligatorio"
        );
    }

    /**
     * Constructor compatible con Factory Method.
     */
    public ProveedorRepuestoService(
            DAOFactory<ProveedorRepuestoDAO> fabrica,
            ProveedorConsulta proveedores,
            RepuestoConsulta repuestos,
            Supplier<Connection> conexiones) {

        this(
                Objects.requireNonNull(
                        fabrica,
                        "La fabrica no puede ser nula"
                ).obtenerDAO(),
                proveedores,
                repuestos,
                conexiones
        );
    }

    // =========================================
    // CREATE - REGISTRAR ASOCIACION
    // =========================================

    public void registrar(
            ProveedorRepuesto asociacion) {

        validarAsociacion(asociacion);
        validarExistenciaEntidades(asociacion);

        enTransaccion(conexion -> {

            dao.insertar(conexion, asociacion);

            return null;
        });
    }

    /**
     * Sobrecarga para registrar usando valores.
     */
    public void registrar(
            int proveedorId,
            int repuestoId,
            BigDecimal costo) {

        ProveedorRepuesto asociacion =
                new ProveedorRepuesto(
                        proveedorId,
                        repuestoId,
                        costo
                );

        registrar(asociacion);
    }

    // =========================================
    // READ - BUSCAR ASOCIACION
    // =========================================

    public Optional<ProveedorRepuesto> buscar(
            int proveedorId,
            int repuestoId) {

        validarId(proveedorId, "proveedor");
        validarId(repuestoId, "repuesto");

        return leer(conexion ->
                dao.buscar(
                        conexion,
                        proveedorId,
                        repuestoId
                )
        );
    }

    // =========================================
    // READ - LISTAR POR PROVEEDOR
    // =========================================

    public List<ProveedorRepuesto> listarPorProveedor(
            int proveedorId) {

        validarId(proveedorId, "proveedor");

        return leer(conexion ->
                dao.listarPorProveedor(
                        conexion,
                        proveedorId
                )
        );
    }

    // =========================================
    // READ - LISTAR POR REPUESTO
    // =========================================

    public List<ProveedorRepuesto> listarPorRepuesto(
            int repuestoId) {

        validarId(repuestoId, "repuesto");

        return leer(conexion ->
                dao.listarPorRepuesto(
                        conexion,
                        repuestoId
                )
        );
    }

    // =========================================
    // UPDATE - ACTUALIZAR COSTO
    // =========================================

    public boolean actualizarCosto(
            ProveedorRepuesto asociacion) {

        validarAsociacion(asociacion);
        validarExistenciaEntidades(asociacion);

        return enTransaccion(conexion ->
                dao.actualizarCosto(
                        conexion,
                        asociacion
                )
        );
    }

    /**
     * Sobrecarga para actualizar usando valores.
     */
    public boolean actualizarCosto(
            int proveedorId,
            int repuestoId,
            BigDecimal nuevoCosto) {

        ProveedorRepuesto asociacion =
                new ProveedorRepuesto(
                        proveedorId,
                        repuestoId,
                        nuevoCosto
                );

        return actualizarCosto(asociacion);
    }

    // =========================================
    // VALIDACIONES
    // =========================================

    private void validarAsociacion(
            ProveedorRepuesto asociacion) {

        if (asociacion == null) {
            throw new ValidacionException(
                    "La asociacion no puede ser nula"
            );
        }

        // El constructor del modelo ya garantiza:
        // - IDs mayores que cero.
        // - Costo no nulo.
        // - Costo no negativo.
        // - Máximo dos decimales.
        // - Rango permitido por DECIMAL(10,2).
    }

    private void validarExistenciaEntidades(
            ProveedorRepuesto asociacion) {

        int proveedorId = asociacion.getProveedorId();
        int repuestoId = asociacion.getRepuestoId();

        if (!proveedores.existe(proveedorId)) {
            throw new ValidacionException(
                    "El proveedor " + proveedorId
                            + " no existe"
            );
        }

        if (!repuestos.existe(repuestoId)) {
            throw new ValidacionException(
                    "El repuesto " + repuestoId
                            + " no existe"
            );
        }
    }

    private void validarId(
            int id,
            String entidad) {

        if (id <= 0) {
            throw new ValidacionException(
                    "El ID del " + entidad
                            + " debe ser mayor que cero"
            );
        }
    }

    // =========================================
    // CONEXIONES DE SOLO LECTURA
    // =========================================

    private <T> T leer(
            Function<Connection, T> accion) {

        try (Connection conexion = conexiones.get()) {

            return accion.apply(conexion);

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al gestionar la conexion de lectura",
                    e
            );
        }
    }

    // =========================================
    // TRANSACCIONES DE ESCRITURA
    // =========================================

    private <T> T enTransaccion(
            Function<Connection, T> accion) {

        try (Connection conexion = conexiones.get()) {

            conexion.setAutoCommit(false);

            try {

                T resultado = accion.apply(conexion);

                conexion.commit();

                return resultado;

            } catch (SQLException | RuntimeException e) {

                revertir(conexion, e);

                if (e instanceof SQLException errorSql) {
                    throw new AccesoDatosException(
                            "Error al confirmar la transaccion",
                            errorSql
                    );
                }

                throw (RuntimeException) e;
            }

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al gestionar la transaccion",
                    e
            );
        }
    }

    private void revertir(
            Connection conexion,
            Exception causa) {

        try {

            conexion.rollback();

        } catch (SQLException errorRollback) {

            causa.addSuppressed(errorRollback);
        }
    }
}
