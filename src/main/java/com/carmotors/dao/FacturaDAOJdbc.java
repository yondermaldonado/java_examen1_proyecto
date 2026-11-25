package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.DatosOrdenParaFactura;
import com.carmotors.modelo.FacturaDTO;
import com.carmotors.modelo.DocumentoFactura;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Consultas y escritura de factura en la conexión transaccional del llamador. */
public final class FacturaDAOJdbc implements FacturaDAO {
    @Override
    public Optional<DatosOrdenParaFactura> buscarOrdenCompletada(Connection con, long ordenId) {
        String estado = """
                SELECT estado.nombre
                FROM servicios orden JOIN servicio_estados estado ON estado.id = orden.estado_id
                WHERE orden.id = ?
                """;
        try (PreparedStatement sentencia = con.prepareStatement(estado)) {
            sentencia.setLong(1, ordenId);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next() || !"Completado".equals(fila.getString(1))) {
                    return Optional.empty();
                }
            }
            return Optional.of(new DatosOrdenParaFactura(repuestos(con, ordenId), manoObra(con, ordenId)));
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar el detalle histórico de la orden.", e);
        }
    }

    private List<DatosOrdenParaFactura.Repuesto> repuestos(Connection con, long ordenId) throws SQLException {
        String consulta = """
                SELECT linea.repuesto_id, repuesto.nombre, linea.cantidad_requerida, linea.precio_unitario_venta
                FROM servicios_repuestos linea JOIN repuestos repuesto ON repuesto.id = linea.repuesto_id
                WHERE linea.servicio_id = ? ORDER BY linea.repuesto_id
                """;
        List<DatosOrdenParaFactura.Repuesto> detalle = new ArrayList<>();
        try (PreparedStatement sentencia = con.prepareStatement(consulta)) {
            sentencia.setLong(1, ordenId);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    detalle.add(new DatosOrdenParaFactura.Repuesto(filas.getLong("repuesto_id"),
                            filas.getString("nombre"), filas.getLong("cantidad_requerida"),
                            filas.getBigDecimal("precio_unitario_venta")));
                }
            }
        }
        return detalle;
    }

    private List<DatosOrdenParaFactura.ManoObra> manoObra(Connection con, long ordenId) throws SQLException {
        String consulta = """
                SELECT detalle.servicio_catalogo_id, catalogo.nombre, detalle.precio_cobrado
                FROM servicios_detalle detalle
                JOIN servicios_catalogo catalogo ON catalogo.id = detalle.servicio_catalogo_id
                WHERE detalle.servicio_id = ? ORDER BY detalle.servicio_catalogo_id
                """;
        List<DatosOrdenParaFactura.ManoObra> detalle = new ArrayList<>();
        try (PreparedStatement sentencia = con.prepareStatement(consulta)) {
            sentencia.setLong(1, ordenId);
            try (ResultSet filas = sentencia.executeQuery()) {
                while (filas.next()) {
                    detalle.add(new DatosOrdenParaFactura.ManoObra(filas.getLong("servicio_catalogo_id"),
                            filas.getString("nombre"), filas.getBigDecimal("precio_cobrado")));
                }
            }
        }
        return detalle;
    }

    @Override
    public Optional<DocumentoFactura> buscarPorOrden(Connection con, long ordenId) {
        String sql = """
                SELECT f.id, f.servicio_id, f.fecha_emision, f.total_repuestos, f.total_mano_obra,
                       f.impuestos, f.total_final, c.nombre AS cliente, c.documento_identidad,
                       c.telefono, c.email, v.placa, m.nombre AS modelo, marca.nombre AS marca
                FROM facturas f JOIN servicios s ON s.id = f.servicio_id
                JOIN vehiculos v ON v.id = s.vehiculo_id JOIN clientes c ON c.id = v.cliente_id
                JOIN modelos_vehiculo m ON m.id = v.modelo_id JOIN marcas_vehiculo marca ON marca.id = m.marca_id
                WHERE f.servicio_id = ?
                """;
        try (var ps = con.prepareStatement(sql)) {
            ps.setLong(1, ordenId);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new DocumentoFactura(rs.getLong("id"), rs.getLong("servicio_id"),
                        rs.getObject("fecha_emision", java.time.LocalDateTime.class), rs.getString("cliente"),
                        rs.getString("documento_identidad"), rs.getString("telefono"), rs.getString("email"),
                        rs.getString("placa"), rs.getString("marca"), rs.getString("modelo"),
                        new DatosOrdenParaFactura(repuestos(con, ordenId), manoObra(con, ordenId)),
                        rs.getBigDecimal("total_repuestos"), rs.getBigDecimal("total_mano_obra"),
                        rs.getBigDecimal("impuestos"), rs.getBigDecimal("total_final")));
            }
        } catch (SQLException e) { throw new DaoException("No se pudo consultar la factura registrada.", e); }
    }

    @Override
    public void insertar(Connection con, FacturaDTO factura, LocalDateTime fechaEmision) {
        String sql = """
                INSERT INTO facturas (servicio_id, fecha_emision, total_repuestos,
                                      total_mano_obra, impuestos, total_final)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement sentencia = con.prepareStatement(sql)) {
            sentencia.setLong(1, factura.ordenId());
            sentencia.setObject(2, fechaEmision);
            sentencia.setBigDecimal(3, factura.totalRepuestos());
            sentencia.setBigDecimal(4, factura.totalManoObra());
            sentencia.setBigDecimal(5, factura.impuestos());
            sentencia.setBigDecimal(6, factura.totalFinal());
            sentencia.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) {
                throw new ValidacionException("La orden ya tiene una factura.");
            }
            throw new DaoException("No se pudo persistir la factura.", e);
        }
    }
}
