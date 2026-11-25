package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.RepuestoMinimo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** DAO JDBC: lee de MySQL los repuestos y sus minimos. No calcula alertas. */
public class AlertasDAOJdbc implements AlertasDAO {
    @Override
    public List<RepuestoMinimo> listarRepuestos(Connection conexion) {
        List<RepuestoMinimo> repuestos = new ArrayList<>();
        // MySQL -> DAO: traemos id, nombre y stock minimo.
        String sql = "SELECT id, nombre, stock_minimo FROM repuestos ORDER BY id";

        try (PreparedStatement sentencia = conexion.prepareStatement(sql);
             ResultSet resultados = sentencia.executeQuery()) {

            while (resultados.next()) {
                int id = resultados.getInt("id");
                String nombre = resultados.getString("nombre");
                int minimo = resultados.getInt("stock_minimo");

                // DAO -> Modelo: cada fila se transforma en un objeto normal.
                RepuestoMinimo repuesto = new RepuestoMinimo(id, nombre, minimo);
                repuestos.add(repuesto);
            }

            return repuestos;
        } catch (SQLException error) {
            throw new DaoException("No se pudieron consultar los minimos de inventario.", error);
        }
    }
}
