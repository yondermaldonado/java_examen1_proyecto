
package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteVehiculosDAOJdbc
        implements ClienteVehiculosDAO {

    @Override
    public boolean tieneVehiculos(
            Connection conexion, int clienteId) {

        String sql = """
            SELECT EXISTS (
                SELECT 1
                FROM vehiculos
                WHERE cliente_id = ?
            )
            """;

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {

                return rs.next() && rs.getBoolean(1);
            }

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al comprobar vehículos del cliente", e
            );
        }
    }
}
