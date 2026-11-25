
package com.carmotors.servicio;

import com.carmotors.dao.ProveedorDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Proveedor;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class ProveedorService implements ProveedorConsulta {

    private final ProveedorDAO proveedorDAO;
    private final Supplier<Connection> conexiones;

    // Constructor que utiliza Factory Method
    public ProveedorService(
            DAOFactory<ProveedorDAO> creador,
            Supplier<Connection> conexiones) {

        this(
                Objects.requireNonNull(
                        creador,
                        "La fabrica no puede ser null"
                ).obtenerDAO(),
                conexiones
        );
    }

    // Constructor para inyectar un DAO directamente
    // Es especialmente util para las pruebas.
    public ProveedorService(
            ProveedorDAO proveedorDAO,
            Supplier<Connection> conexiones) {

        this.proveedorDAO = Objects.requireNonNull(
                proveedorDAO,
                "El DAO no puede ser null"
        );

        this.conexiones = Objects.requireNonNull(
                conexiones,
                "El proveedor de conexiones no puede ser null"
        );
    }

    // =========================================
    // CREATE - REGISTRAR
    // =========================================

    public Proveedor registrar(Proveedor proveedor) {

        Objects.requireNonNull(
                proveedor,
                "El proveedor no puede ser null"
        );

        if (proveedor.getId() != 0) {
            throw new IllegalArgumentException(
                    "El proveedor ya tiene un ID asignado"
            );
        }

        // Trabajamos con una copia para que un rollback
        // no deje el objeto original con ID asignado.
        Proveedor copia = new Proveedor(
                proveedor.getNombre(),
                proveedor.getContacto(),
                proveedor.getEmail()
        );

        Proveedor registrado = enTransaccion(
                conexion -> proveedorDAO.insertar(conexion, copia)
        );

        // Solo se asigna despues de un commit exitoso.
        proveedor.setId(registrado.getId());

        return proveedor;
    }

    // =========================================
    // READ - CONSULTAS
    // =========================================

    public Optional<Proveedor> buscarPorId(int id) {

        validarId(id);

        return leer(
                conexion -> proveedorDAO.buscarPorId(conexion, id)
        );
    }

    public List<Proveedor> buscarPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre de busqueda no puede estar vacio"
            );
        }

        return leer(
                conexion -> proveedorDAO.buscarPorNombre(
                        conexion, nombre.strip()
                )
        );
    }

    public List<Proveedor> buscarPorEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo de busqueda no puede estar vacio"
            );
        }

        String emailLimpio = email.strip()
                .toLowerCase(Locale.ROOT);

        return leer(
                conexion -> proveedorDAO.buscarPorEmail(
                        conexion, emailLimpio
                )
        );
    }

    public List<Proveedor> listar() {

        return leer(
                proveedorDAO::listar
        );
    }

    // Implementacion del contrato que usa inventario
    @Override
    public boolean existe(int proveedorId) {

        if (proveedorId <= 0) {
            return false;
        }

        return leer(
                conexion -> proveedorDAO.existe(
                        conexion, proveedorId
                )
        );
    }

    // =========================================
    // UPDATE - ACTUALIZAR
    // =========================================

    public boolean actualizar(Proveedor proveedor) {

        Objects.requireNonNull(
                proveedor,
                "El proveedor no puede ser null"
        );

        validarId(proveedor.getId());

        return enTransaccion(
                conexion -> proveedorDAO.actualizar(
                        conexion, proveedor
                )
        );
    }

    // Permite actualizar creando un objeto validado
    // con los nuevos datos.
    public boolean actualizar(
            int id,
            String nombre,
            String contacto,
            String email) {

        validarId(id);

        Proveedor proveedor = new Proveedor(
                nombre, contacto, email
        );

        proveedor.setId(id);

        return actualizar(proveedor);
    }

    // =========================================
    // DELETE - ELIMINAR
    // =========================================

    public boolean eliminar(int id) {

        validarId(id);

        return enTransaccion(conexion -> {

            try {
                return proveedorDAO.eliminar(conexion, id);

            } catch (AccesoDatosException e) {

                if (esErrorMysql(e, 1451)) {
                    throw new ValidacionException(
                            "No se puede eliminar el proveedor "
                                    + "porque tiene repuestos, lotes "
                                    + "o evaluaciones asociados"
                    );
                }

                throw e;
            }
        });
    }

    // =========================================
    // VALIDACIONES
    // =========================================

    private void validarId(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor que cero"
            );
        }
    }

    private boolean esErrorMysql(
            AccesoDatosException error,
            int codigo) {

        return error.getCause() instanceof SQLException sql
                && sql.getErrorCode() == codigo;
    }

    // =========================================
    // CONEXIONES Y TRANSACCIONES
    // =========================================

    // Para operaciones de lectura.
    private <T> T leer(Function<Connection, T> accion) {

        try (Connection conexion = conexiones.get()) {

            return accion.apply(conexion);

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la conexion", e
            );
        }
    }

    // Para operaciones que modifican datos.
    private <T> T enTransaccion(
            Function<Connection, T> accion) {

        try (Connection conexion = conexiones.get()) {

            conexion.setAutoCommit(false);

            try {
                T resultado = accion.apply(conexion);

                conexion.commit();

                return resultado;

            } catch (SQLException e) {

                revertir(conexion, e);

                throw new AccesoDatosException(
                        "Error al confirmar la transaccion", e
                );

            } catch (RuntimeException e) {

                revertir(conexion, e);

                throw e;
            }

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al gestionar la conexion", e
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
