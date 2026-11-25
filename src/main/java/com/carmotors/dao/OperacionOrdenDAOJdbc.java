package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.OperacionOrden;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class OperacionOrdenDAOJdbc implements OperacionOrdenDAO {
    @Override
    public void registrar(Connection con, int ordenId, int catalogoId, BigDecimal precio) {
        String sql = "INSERT INTO servicios_detalle(servicio_id, servicio_catalogo_id, precio_cobrado) VALUES (?, ?, ?)";
        try (var ps = con.prepareStatement(sql)) {
            ps.setInt(1, ordenId); ps.setInt(2, catalogoId); ps.setBigDecimal(3, precio);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) throw new ValidacionException("La operación ya está registrada; utiliza actualizar precio.");
            throw new DaoException("No se pudo registrar la operación.", e);
        }
    }

    @Override
    public boolean actualizar(Connection con, int ordenId, int catalogoId, BigDecimal precio) {
        try (var ps = con.prepareStatement("UPDATE servicios_detalle SET precio_cobrado = ? WHERE servicio_id = ? AND servicio_catalogo_id = ?")) {
            ps.setBigDecimal(1, precio); ps.setInt(2, ordenId); ps.setInt(3, catalogoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudo actualizar el precio de la operación.", e); }
    }

    @Override
    public boolean quitar(Connection con, int ordenId, int catalogoId) {
        try (var ps = con.prepareStatement("DELETE FROM servicios_detalle WHERE servicio_id = ? AND servicio_catalogo_id = ?")) {
            ps.setInt(1, ordenId); ps.setInt(2, catalogoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudo retirar la operación.", e); }
    }

    @Override
    public List<OperacionOrden> listar(Connection con, int ordenId) {
        String sql = "SELECT d.servicio_catalogo_id, c.nombre, d.precio_cobrado FROM servicios_detalle d "
                + "JOIN servicios_catalogo c ON c.id = d.servicio_catalogo_id WHERE d.servicio_id = ? ORDER BY c.nombre, c.id";
        try (var ps = con.prepareStatement(sql)) {
            ps.setInt(1, ordenId);
            try (var rs = ps.executeQuery()) {
                List<OperacionOrden> resultado = new ArrayList<>();
                while (rs.next()) resultado.add(new OperacionOrden(rs.getInt(1), rs.getString(2), rs.getBigDecimal(3)));
                return resultado;
            }
        } catch (SQLException e) { throw new DaoException("No se pudieron consultar las operaciones de la orden.", e); }
    }
}
