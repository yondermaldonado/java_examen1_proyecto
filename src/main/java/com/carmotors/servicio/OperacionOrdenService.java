package com.carmotors.servicio;

import com.carmotors.dao.OperacionOrdenDAO;
import com.carmotors.dao.OrdenServicioDAO;
import com.carmotors.dao.ServicioCatalogoDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.OperacionOrden;
import com.carmotors.modelo.ServicioCatalogo;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/** Mantiene los trabajos y precios históricos; comparte el bloqueo de las transiciones de la orden. */
public final class OperacionOrdenService {
    private final OperacionOrdenDAO operaciones;
    private final ServicioCatalogoDAO catalogo;
    private final OrdenServicioDAO ordenes;
    private final Supplier<Connection> conexiones;

    public OperacionOrdenService(OperacionOrdenDAO operaciones, ServicioCatalogoDAO catalogo,
                                 OrdenServicioDAO ordenes, Supplier<Connection> conexiones) {
        this.operaciones = Objects.requireNonNull(operaciones);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.conexiones = Objects.requireNonNull(conexiones);
    }

    public List<ServicioCatalogo> listarCatalogo() { return consultar(catalogo::listar); }

    public List<OperacionOrden> listar(int ordenId) {
        positivo(ordenId);
        return consultar(con -> {
            if (ordenes.buscarPorId(con, ordenId).isEmpty()) throw new ValidacionException("No existe la orden indicada.");
            return operaciones.listar(con, ordenId);
        });
    }

    public void registrar(int ordenId, int catalogoId, BigDecimal precio) {
        validar(ordenId, catalogoId, precio);
        escribir(con -> {
            abierta(con, ordenId);
            if (catalogo.listar(con).stream().noneMatch(c -> c.id() == catalogoId)) {
                throw new ValidacionException("No existe la operación en el catálogo.");
            }
            operaciones.registrar(con, ordenId, catalogoId, precio.setScale(2));
            return null;
        });
    }

    public void actualizar(int ordenId, int catalogoId, BigDecimal precio) {
        validar(ordenId, catalogoId, precio);
        escribir(con -> {
            abierta(con, ordenId);
            if (operaciones.listar(con, ordenId).stream().noneMatch(o -> o.catalogoId() == catalogoId)) {
                throw new ValidacionException("La operación no está registrada en esta orden.");
            }
            // Actualizar al mismo importe es válido aunque MySQL informe cero filas cambiadas.
            operaciones.actualizar(con, ordenId, catalogoId, precio.setScale(2));
            return null;
        });
    }

    public void quitar(int ordenId, int catalogoId) {
        positivo(ordenId); positivo(catalogoId);
        escribir(con -> {
            abierta(con, ordenId);
            if (!operaciones.quitar(con, ordenId, catalogoId)) throw new ValidacionException("La operación no está registrada en esta orden.");
            return null;
        });
    }

    private void abierta(Connection con, int ordenId) {
        var orden = ordenes.bloquearPorId(con, ordenId)
                .orElseThrow(() -> new ValidacionException("No existe la orden indicada."));
        if (!List.of("Pendiente", "En Proceso").contains(orden.getEstado())) {
            throw new ValidacionException("Solo se pueden modificar operaciones de órdenes Pendiente o En Proceso.");
        }
    }

    private void validar(int orden, int catalogo, BigDecimal precio) {
        positivo(orden); positivo(catalogo);
        if (precio == null || precio.signum() < 0 || precio.stripTrailingZeros().scale() > 2
                || precio.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new ValidacionException("El precio debe estar entre 0 y 99999999.99 y tener hasta dos decimales.");
        }
    }
    private void positivo(int id) { if (id <= 0) throw new ValidacionException("Los identificadores deben ser positivos."); }

    private <T> T consultar(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) { return accion.apply(con); }
        catch (SQLException e) { throw new DaoException("No se pudo cerrar la consulta de operaciones.", e); }
    }

    private <T> T escribir(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                T resultado = accion.apply(con);
                con.commit();
                return resultado;
            } catch (RuntimeException | SQLException e) {
                try { con.rollback(); } catch (SQLException fallo) { e.addSuppressed(fallo); }
                if (e instanceof RuntimeException runtime) throw runtime;
                throw new DaoException("No se pudo confirmar la operación de mano de obra.", e);
            }
        } catch (SQLException e) { throw new DaoException("No se pudo administrar la transacción de operaciones.", e); }
    }
}
