package com.carmotors.servicio;

import com.carmotors.dao.TecnicoDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Tecnico;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/** Registro y consulta de técnicos. La transacción pertenece a este servicio. */
public class TecnicoService {
    private final TecnicoDAO dao;
    private final Supplier<Connection> conexiones;

    public TecnicoService(DAOFactory<TecnicoDAO> creador, Supplier<Connection> conexiones) {
        this(creador.obtenerDAO(), conexiones);
    }

    public TecnicoService(TecnicoDAO dao, Supplier<Connection> conexiones) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    public int registrar(Tecnico tecnico) {
        validar(tecnico);
        return transaccion(con -> {
            documentoDisponible(con, tecnico, 0);
            return dao.crear(con, tecnico);
        });
    }

    public List<Tecnico> listar() {
        return consultar(dao::listar);
    }

    public Optional<Tecnico> buscarPorId(int id) {
        validarId(id);
        return consultar(con -> dao.buscarPorId(con, id));
    }

    public Optional<Tecnico> buscarPorDocumento(String documento) {
        String normalizado = texto(documento, "documento", 20, true);
        return consultar(con -> dao.buscarPorDocumento(con, normalizado));
    }

    public boolean actualizar(Tecnico tecnico) {
        validar(tecnico);
        validarId(tecnico.getId());
        return transaccion(con -> {
            if (dao.buscarPorId(con, tecnico.getId()).isEmpty()) return false;
            documentoDisponible(con, tecnico, tecnico.getId());
            return dao.actualizar(con, tecnico);
        });
    }

    public boolean eliminar(int id) {
        validarId(id);
        return transaccion(con -> {
            if (dao.tieneOrdenes(con, id)) {
                throw new ValidacionException("El técnico tiene órdenes relacionadas. Desactívalo en lugar de eliminarlo.");
            }
            return dao.eliminar(con, id);
        });
    }

    private void documentoDisponible(Connection con, Tecnico tecnico, int idPermitido) {
        dao.buscarPorDocumento(con, tecnico.getDocumentoIdentidad()).ifPresent(existente -> {
            if (existente.getId() != idPermitido) {
                throw new ValidacionException("Ya existe un técnico con ese documento.");
            }
        });
    }

    private void validar(Tecnico tecnico) {
        if (tecnico == null) throw new ValidacionException("Debes proporcionar los datos del técnico.");
        tecnico.setNombre(texto(tecnico.getNombre(), "nombre", 255, true));
        tecnico.setDocumentoIdentidad(texto(tecnico.getDocumentoIdentidad(), "documento", 20, true));
        tecnico.setTelefono(texto(tecnico.getTelefono(), "teléfono", 20, false));
        tecnico.setEspecialidad(texto(tecnico.getEspecialidad(), "especialidad", 150, false));
    }

    private String texto(String valor, String campo, int maximo, boolean obligatorio) {
        String limpio = valor == null ? "" : valor.trim();
        if (obligatorio && limpio.isEmpty()) throw new ValidacionException("El " + campo + " es obligatorio.");
        if (limpio.codePointCount(0, limpio.length()) > maximo) {
            throw new ValidacionException("El " + campo + " supera " + maximo + " caracteres.");
        }
        return limpio.isEmpty() ? null : limpio;
    }

    private void validarId(int id) {
        if (id <= 0) throw new ValidacionException("El id del técnico debe ser mayor que cero.");
    }

    private <T> T consultar(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            return accion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión de técnicos.", e);
        }
    }

    private <T> T transaccion(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                T resultado = accion.apply(con);
                con.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                try { con.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                // MySQL garantiza unicidad y referencias incluso frente a cambios concurrentes.
                for (Throwable causa = e; causa != null; causa = causa.getCause()) {
                    if (causa instanceof SQLException sql) {
                        if (sql.getErrorCode() == 1062) throw new ValidacionException("Ya existe un técnico con ese documento.");
                        if (sql.getErrorCode() == 1451) throw new ValidacionException("El técnico tiene órdenes relacionadas y no se puede eliminar.");
                    }
                }
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new DaoException("No se pudo confirmar la operación de técnicos.", e);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo administrar la conexión de técnicos.", e);
        }
    }
}
