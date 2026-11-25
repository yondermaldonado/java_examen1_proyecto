package com.carmotors.servicio;

import com.carmotors.dao.RepuestoTipoDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SERVICE: aplica validaciones y administra las conexiones y transacciones.
 * Recibe solicitudes de RepuestoTipoController y llama a RepuestoTipoDAO.
 * Se depende de la interfaz DAO, no de RepuestoTipoDAOJdbc (principio DIP).
 */
public final class RepuestoTipoService {

    private final RepuestoTipoDAO dao;
    private final Supplier<Connection> conexiones;

    public RepuestoTipoService(RepuestoTipoDAO dao, Supplier<Connection> conexiones) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    // 1. Validar nombre. 2. Abrir transacción. 3. Pedir al DAO insertar.
    public int crear(String nombre) {
        String nombreValido = validarNombre(nombre);
        return escribir(conexion -> dao.crear(conexion, nombreValido));
    }

    // Valida ID y nombre, luego solicita el UPDATE al DAO.
    public boolean actualizar(int id, String nombre) {
        validarId(id);
        String nombreValido = validarNombre(nombre);
        return escribir(conexion -> dao.actualizar(conexion, id, nombreValido));
    }

    // Valida ID y solicita el DELETE. MySQL impide borrar tipos en uso.
    public boolean eliminar(int id) {
        validarId(id);
        return escribir(conexion -> dao.eliminar(conexion, id));
    }

    // Solo consulta: no modifica registros ni requiere commit.
    public List<RepuestoTipo> listar() {
        return leer(conexion -> dao.listar(conexion));
    }

    public Optional<RepuestoTipo> buscarPorId(int id) {
        validarId(id);
        return leer(conexion -> dao.buscarPorId(conexion, id));
    }

    public List<RepuestoTipo> buscarPorNombre(String nombre) {
        String textoBuscado = "";
        if (nombre != null) {
            textoBuscado = nombre.trim();
        }
        String busqueda = textoBuscado;
        return leer(conexion -> dao.buscarPorNombre(conexion, busqueda));
    }

    // Reglas de negocio: los identificadores deben ser positivos.
    private void validarId(int id) {
        if (id <= 0) {
            throw new ValidacionException("El ID debe ser mayor que cero.");
        }
    }

    // Reglas de negocio: nombre obligatorio, sin espacios extremos, max 100.
    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre del tipo es obligatorio.");
        }

        String nombreLimpio = nombre.trim();
        if (nombreLimpio.length() > 100) {
            throw new ValidacionException("El nombre del tipo no puede superar 100 caracteres.");
        }
        return nombreLimpio;
    }

    // Abre una conexión, llama al DAO mediante la lambda y cierra la conexión.
    // <T> permite reutilizar el método para List, Optional, etc.
    private <T> T leer(Function<Connection, T> operacion) {
        try (Connection conexion = conexiones.get()) {
            return operacion.apply(conexion);
        } catch (SQLException e) {
            throw new DaoException("Error al cerrar la conexión de lectura de tipos.", e);
        }
    }

    // Solo para crear, actualizar y eliminar.
    // Si el DAO termina bien: commit. Si falla: rollback.
    private <T> T escribir(Function<Connection, T> operacion) {
        try (Connection conexion = conexiones.get()) {
            conexion.setAutoCommit(false);

            try {
                T resultado = operacion.apply(conexion);
                conexion.commit();
                return resultado;
            } catch (RuntimeException | SQLException error) {
                try {
                    conexion.rollback();
                } catch (SQLException errorRollback) {
                    error.addSuppressed(errorRollback);
                }

                if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                }
                throw new DaoException("No se pudo confirmar la transacción de tipos.", error);
            }
        } catch (SQLException e) {
            throw new DaoException("Error al abrir o cerrar la transacción de tipos.", e);
        }
    }
}
