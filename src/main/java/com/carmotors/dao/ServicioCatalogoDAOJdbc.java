package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.ServicioCatalogo;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class ServicioCatalogoDAOJdbc implements ServicioCatalogoDAO {
    @Override
    public List<ServicioCatalogo> listar(Connection con) {
        String sql = "SELECT id, nombre, precio_base, intervalo_meses FROM servicios_catalogo ORDER BY nombre, id";
        try (var ps = con.prepareStatement(sql); var rs = ps.executeQuery()) {
            List<ServicioCatalogo> resultado = new ArrayList<>();
            while (rs.next()) {
                resultado.add(new ServicioCatalogo(rs.getInt("id"), rs.getString("nombre"),
                        rs.getBigDecimal("precio_base"), rs.getObject("intervalo_meses", Integer.class)));
            }
            return resultado;
        } catch (SQLException e) { throw new DaoException("No se pudo consultar el catálogo de operaciones.", e); }
    }
}
