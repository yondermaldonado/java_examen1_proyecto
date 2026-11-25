package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.OrdenServicio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OrdenServicioDAOJdbc implements OrdenServicioDAO {
    private static final String CONSULTA = "SELECT s.id, s.vehiculo_id, v.cliente_id, v.placa, "
            + "mt.nombre AS tipo, e.nombre AS estado, s.fecha_recepcion, s.fecha_completado, "
            + "s.fecha_entrega, s.descripcion_problema, "
            + "COALESCE((SELECT SUM(d.precio_cobrado) FROM servicios_detalle d WHERE d.servicio_id = s.id), 0) AS mano_obra "
            + "FROM servicios s JOIN vehiculos v ON v.id = s.vehiculo_id "
            + "JOIN mantenimiento_tipos mt ON mt.id = s.mantenimiento_tipo_id "
            + "JOIN servicio_estados e ON e.id = s.estado_id ";

    @Override
    public int crear(Connection con, OrdenServicio orden) {
        String sql = "INSERT INTO servicios (vehiculo_id, mantenimiento_tipo_id, estado_id, fecha_recepcion, descripcion_problema) "
                + "SELECT ?, mt.id, e.id, ?, ? FROM mantenimiento_tipos mt CROSS JOIN servicio_estados e "
                + "WHERE mt.nombre = ? AND e.nombre = ?";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, orden.getVehiculoId());
            ps.setTimestamp(2, Timestamp.valueOf(orden.getFechaRecepcion()));
            ps.setString(3, orden.getDescripcionProblema());
            ps.setString(4, orden.getTipoMantenimiento());
            ps.setString(5, orden.getEstado());
            if (ps.executeUpdate() != 1) {
                throw new DaoException("Faltan catálogos de mantenimiento/estado. Ejecuta database/actualizar-estados-ordenes.sql.");
            }
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el id de la orden.");
                return claves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo registrar la orden de servicio.", e);
        }
    }

    @Override
    public Optional<OrdenServicio> bloquearPorId(Connection con, int id) {
        // Bloquear la misma fila de servicios que utiliza el módulo de asignaciones.
        try (PreparedStatement ps = con.prepareStatement("SELECT id FROM servicios WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
            }
            return buscarPorId(con, id);
        } catch (SQLException e) { throw new DaoException("No se pudo bloquear la orden.", e); }
    }

    @Override
    public boolean actualizarEstado(Connection con, int id, String actual, String nuevo, LocalDateTime fecha) {
        String sql = "UPDATE servicios SET estado_id = (SELECT id FROM servicio_estados WHERE nombre = ?), "
                + "fecha_completado = CASE WHEN ? = 'Completado' THEN ? ELSE fecha_completado END, "
                + "fecha_entrega = CASE WHEN ? = 'Entregado' THEN ? ELSE fecha_entrega END "
                + "WHERE id = ? AND estado_id = (SELECT id FROM servicio_estados WHERE nombre = ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nuevo); ps.setString(2, nuevo); ps.setTimestamp(3, Timestamp.valueOf(fecha));
            ps.setString(4, nuevo); ps.setTimestamp(5, Timestamp.valueOf(fecha)); ps.setInt(6, id); ps.setString(7, actual);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) { throw new DaoException("No se pudo actualizar el estado de la orden. Comprueba el catálogo de estados.", e); }
    }

    @Override
    public boolean existeFactura(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM facturas WHERE servicio_id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DaoException("No se pudo verificar la factura de la orden.", e); }
    }

    @Override
    public boolean consumoCompleto(Connection con, int id) {
        String sql = "SELECT 1 FROM servicios_repuestos sr WHERE sr.servicio_id = ? AND ("
                + "sr.cantidad_requerida <> COALESCE((SELECT SUM(r.cantidad) FROM reservas_repuestos r "
                + "JOIN reserva_estados e ON e.id = r.estado_id WHERE r.servicio_id = sr.servicio_id "
                + "AND r.repuesto_id = sr.repuesto_id AND e.nombre = 'CONSUMIDA'), 0) "
                + "OR EXISTS (SELECT 1 FROM reservas_repuestos r JOIN reserva_estados e ON e.id = r.estado_id "
                + "WHERE r.servicio_id = sr.servicio_id AND r.repuesto_id = sr.repuesto_id AND e.nombre = 'ACTIVA')) LIMIT 1";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) { return !rs.next(); }
        } catch (SQLException e) { throw new DaoException("No se pudo comprobar el consumo de la orden.", e); }
    }

    @Override public Optional<OrdenServicio> buscarPorId(Connection con, int id) {
        return consultar(con, "WHERE s.id = ?", id).stream().findFirst();
    }
    @Override public List<OrdenServicio> listar(Connection con) {
        return consultar(con, "", null);
    }
    @Override public List<OrdenServicio> listarPorVehiculo(Connection con, int id) {
        return consultar(con, "WHERE s.vehiculo_id = ?", id);
    }
    @Override public List<OrdenServicio> listarPorEstado(Connection con, String estado) {
        return consultar(con, "WHERE e.nombre = ?", estado);
    }

    private List<OrdenServicio> consultar(Connection con, String filtro, Object valor) {
        try (PreparedStatement ps = con.prepareStatement(CONSULTA + filtro + " ORDER BY s.fecha_recepcion, s.id")) {
            if (valor instanceof Integer id) ps.setInt(1, id);
            else if (valor instanceof String texto) ps.setString(1, texto);
            try (ResultSet rs = ps.executeQuery()) {
                List<OrdenServicio> ordenes = new ArrayList<>();
                while (rs.next()) {
                    OrdenServicio o = new OrdenServicio();
                    o.setId(rs.getInt("id")); o.setVehiculoId(rs.getInt("vehiculo_id"));
                    o.setClienteId(rs.getInt("cliente_id")); o.setPlaca(rs.getString("placa"));
                    o.setTipoMantenimiento(rs.getString("tipo")); o.setEstado(rs.getString("estado"));
                    o.setFechaRecepcion(rs.getTimestamp("fecha_recepcion").toLocalDateTime());
                    Timestamp completado = rs.getTimestamp("fecha_completado");
                    Timestamp entrega = rs.getTimestamp("fecha_entrega");
                    o.setFechaCompletado(completado == null ? null : completado.toLocalDateTime());
                    o.setFechaEntrega(entrega == null ? null : entrega.toLocalDateTime());
                    o.setDescripcionProblema(rs.getString("descripcion_problema"));
                    o.setCostoManoObra(rs.getBigDecimal("mano_obra"));
                    ordenes.add(o);
                }
                return ordenes;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar las órdenes.", e);
        }
    }
}
