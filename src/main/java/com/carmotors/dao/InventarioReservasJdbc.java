package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.LineaRepuestoOrden;
import com.carmotors.modelo.ReservaOrden;
import com.carmotors.servicio.InventarioReservas;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** MySQL/InnoDB. No usa snapshots antiguos para calcular reservas concurrentes. */
public class InventarioReservasJdbc implements InventarioReservas {
    @Override
    public Optional<BigDecimal> bloquearPrecio(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT precio_venta FROM repuestos WHERE id = ? FOR UPDATE")) {
            ps.setInt(1,id);
            try (ResultSet rs=ps.executeQuery()) { return rs.next()?Optional.of(rs.getBigDecimal(1)):Optional.empty(); }
        } catch (SQLException e) { throw error(e); }
    }

    @Override
    public List<LineaRepuestoOrden> bloquearLineas(Connection con, int orden) {
        try (PreparedStatement ps=con.prepareStatement("SELECT repuesto_id, cantidad_requerida, precio_unitario_venta FROM servicios_repuestos WHERE servicio_id = ? ORDER BY repuesto_id FOR UPDATE")) {
            ps.setInt(1,orden);
            try(ResultSet rs=ps.executeQuery()) {
                List<LineaRepuestoOrden> result=new ArrayList<>();
                while(rs.next()) result.add(new LineaRepuestoOrden(rs.getInt(1),rs.getInt(2),rs.getBigDecimal(3)));
                return result;
            }
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public List<ExistenciaLote> bloquearLotesDisponibles(Connection con, int repuesto) {
        String sql="SELECT lr.lote_id, l.codigo, l.proveedor_id, l.fecha_ingreso, lr.cantidad_ingresada, "
                +"lr.cantidad_actual, lr.costo_unitario, lr.fecha_caducidad FROM lotes_repuestos lr "
                +"JOIN lotes l ON l.id=lr.lote_id WHERE lr.repuesto_id=? ORDER BY lr.lote_id FOR UPDATE";
        List<ExistenciaLote> filas=new ArrayList<>();
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,repuesto);
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) {
                    int lote=rs.getInt("lote_id"),fisico=rs.getInt("cantidad_actual");
                    long reservado=reservadoActual(con,lote,repuesto);
                    if(reservado>fisico) throw new DaoException("El lote tiene más reservas activas que existencias físicas.");
                    var caducidad=rs.getDate("fecha_caducidad");
                    filas.add(new ExistenciaLote(lote,rs.getString("codigo"),rs.getInt("proveedor_id"),null,
                            rs.getDate("fecha_ingreso").toLocalDate(),repuesto,null,rs.getInt("cantidad_ingresada"),fisico,
                            Math.toIntExact(fisico-reservado),rs.getBigDecimal("costo_unitario"),
                            caducidad==null?null:caducidad.toLocalDate()));
                }
            }
            return filas;
        } catch(SQLException e) { throw error(e); }
    }

    private long reservadoActual(Connection con,int lote,int repuesto) throws SQLException {
        // FOR UPDATE obliga lectura actual incluso en REPEATABLE READ con snapshot previo.
        String sql="SELECT r.cantidad FROM reservas_repuestos r JOIN reserva_estados e ON e.id=r.estado_id "
                +"WHERE r.lote_id=? AND r.repuesto_id=? AND e.nombre='ACTIVA' ORDER BY r.id FOR UPDATE";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,lote);ps.setInt(2,repuesto);
            try(ResultSet rs=ps.executeQuery()) {
                long total=0;while(rs.next()) total=Math.addExact(total,rs.getLong(1));return total;
            }
        }
    }

    @Override
    public List<ReservaOrden> bloquearReservas(Connection con,int orden,int repuesto) {
        String sql="SELECT r.id,r.repuesto_id,r.lote_id,r.cantidad,e.nombre,r.fecha_reserva FROM reservas_repuestos r "
                +"JOIN reserva_estados e ON e.id=r.estado_id WHERE r.servicio_id=? AND r.repuesto_id=? ORDER BY r.lote_id,r.id FOR UPDATE";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);ps.setInt(2,repuesto);
            try(ResultSet rs=ps.executeQuery()) {
                List<ReservaOrden> filas=new ArrayList<>();
                while(rs.next()) filas.add(new ReservaOrden(rs.getInt(1),rs.getInt(2),rs.getInt(3),rs.getInt(4),rs.getString(5),rs.getTimestamp(6).toLocalDateTime()));
                return filas;
            }
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public void guardarLinea(Connection con,int orden,LineaRepuestoOrden linea) {
        // Al ajustar no se reemplaza el precio histórico con el precio actual del catálogo.
        String sql="INSERT INTO servicios_repuestos (servicio_id,repuesto_id,cantidad_requerida,precio_unitario_venta) "
                +"VALUES (?,?,?,?) ON DUPLICATE KEY UPDATE cantidad_requerida=VALUES(cantidad_requerida)";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);ps.setInt(2,linea.repuestoId());ps.setInt(3,linea.cantidad());ps.setBigDecimal(4,linea.precioUnitario());ps.executeUpdate();
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public void liberarActivas(Connection con,int orden,int repuesto) {
        String sql="UPDATE reservas_repuestos SET estado_id=(SELECT id FROM reserva_estados WHERE nombre='LIBERADA') "
                +"WHERE servicio_id=? AND repuesto_id=? AND estado_id=(SELECT id FROM reserva_estados WHERE nombre='ACTIVA')";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);ps.setInt(2,repuesto);ps.executeUpdate();
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public void reservar(Connection con,int orden,AsignacionLote lote,LocalDateTime fecha) {
        String sql="INSERT INTO reservas_repuestos (servicio_id,repuesto_id,lote_id,estado_id,cantidad,fecha_reserva) "
                +"SELECT ?,?,?,id,?,? FROM reserva_estados WHERE nombre='ACTIVA'";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setInt(1,orden);ps.setInt(2,lote.repuestoId());ps.setInt(3,lote.loteId());ps.setInt(4,lote.cantidad());ps.setTimestamp(5,Timestamp.valueOf(fecha));
            if(ps.executeUpdate()!=1) throw new DaoException("Falta ACTIVA en el catálogo de reservas.");
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public boolean descontarLote(Connection con,int lote,int repuesto,int cantidad) {
        try(PreparedStatement ps=con.prepareStatement("UPDATE lotes_repuestos SET cantidad_actual=cantidad_actual-? WHERE lote_id=? AND repuesto_id=? AND cantidad_actual>=?")) {
            ps.setInt(1,cantidad);ps.setInt(2,lote);ps.setInt(3,repuesto);ps.setInt(4,cantidad);return ps.executeUpdate()==1;
        } catch(SQLException e) { throw error(e); }
    }

    @Override
    public boolean consumirReserva(Connection con,int reserva,LocalDateTime fecha) {
        String sql="UPDATE reservas_repuestos SET estado_id=(SELECT id FROM reserva_estados WHERE nombre='CONSUMIDA'),fecha_consumo=? "
                +"WHERE id=? AND estado_id=(SELECT id FROM reserva_estados WHERE nombre='ACTIVA')";
        try(PreparedStatement ps=con.prepareStatement(sql)) {
            ps.setTimestamp(1,Timestamp.valueOf(fecha));ps.setInt(2,reserva);return ps.executeUpdate()==1;
        } catch(SQLException e) { throw error(e); }
    }

    private DaoException error(SQLException e) { return new DaoException("No se pudo operar con las reservas de inventario.",e); }
}
