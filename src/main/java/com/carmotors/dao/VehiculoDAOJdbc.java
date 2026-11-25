package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VehiculoDAOJdbc implements VehiculoDAO {

    private static final String COLUMNAS = "id, cliente_id, modelo_id, placa";

    // CREATE: MySQL genera el ID; el Service se encarga del commit.
    @Override
    public Vehiculo insertar(Connection conexion, Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "El vehículo no puede ser null");
        if (vehiculo.getId() != 0) {
            throw new IllegalArgumentException("El vehículo ya tiene un ID asignado");
        }

        String sql = """
                INSERT INTO vehiculos (cliente_id, modelo_id, placa)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, vehiculo.getClienteId());
            ps.setInt(2, vehiculo.getModeloId());
            ps.setString(3, vehiculo.getPlaca());

            int filas = ps.executeUpdate();
            if (filas != 1) {
                throw new AccesoDatosException("No se pudo insertar el vehículo");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new AccesoDatosException("MySQL no devolvió el ID del vehículo");
                }

                // ID provisional: podría revertirse la transacción.
                vehiculo.setId(rs.getInt(1));
                return vehiculo;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al insertar el vehículo", e);
        }
    }

    // READ: buscar por ID.
    @Override
    public Optional<Vehiculo> buscarPorId(Connection conexion, int id) {
        String sql = "SELECT " + COLUMNAS + " FROM vehiculos WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearVehiculo(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar vehículo por ID", e);
        }
    }

    // READ: buscar por placa. La normalización la hace el Service.
    @Override
    public Optional<Vehiculo> buscarPorPlaca(Connection conexion, String placa) {
        String sql = "SELECT " + COLUMNAS + " FROM vehiculos WHERE placa = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, placa);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearVehiculo(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar vehículo por placa", e);
        }
    }

    // READ: listar todos.
    @Override
    public List<Vehiculo> listar(Connection conexion) {
        String sql = "SELECT " + COLUMNAS + " FROM vehiculos ORDER BY id";
        List<Vehiculo> vehiculos = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                vehiculos.add(mapearVehiculo(rs));
            }
            return vehiculos;

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar vehículos", e);
        }
    }

    // READ: un cliente puede tener varios vehículos.
    @Override
    public List<Vehiculo> listarPorCliente(Connection conexion, int clienteId) {
        String sql = "SELECT " + COLUMNAS
                + " FROM vehiculos WHERE cliente_id = ? ORDER BY id";
        List<Vehiculo> vehiculos = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    vehiculos.add(mapearVehiculo(rs));
                }
            }
            return vehiculos;

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al listar vehículos del cliente", e);
        }
    }

    // UPDATE: permite modificar placa, propietario o modelo.
    @Override
    public boolean actualizar(Connection conexion, Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "El vehículo no puede ser null");
        if (vehiculo.getId() <= 0) {
            throw new IllegalArgumentException("El vehículo necesita un ID válido");
        }

        String sql = """
                UPDATE vehiculos
                SET cliente_id = ?, modelo_id = ?, placa = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, vehiculo.getClienteId());
            ps.setInt(2, vehiculo.getModeloId());
            ps.setString(3, vehiculo.getPlaca());
            ps.setInt(4, vehiculo.getId());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                return true;
            }

            // MySQL puede reportar 0 si los valores no cambiaron.
            return buscarPorId(conexion, vehiculo.getId()).isPresent();

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al actualizar vehículo", e);
        }
    }

    // DELETE: las relaciones con servicios las protege MySQL.
    @Override
    public boolean eliminar(Connection conexion, int id) {
        String sql = "DELETE FROM vehiculos WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al eliminar vehículo", e);
        }
    }

    // Lectura del catálogo; no requiere crear una clase ModeloVehiculo.
    @Override
    public boolean existeModelo(Connection conexion, int modeloId) {
        String sql = "SELECT EXISTS (SELECT 1 FROM modelos_vehiculo WHERE id = ?)";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, modeloId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean(1);
            }

        } catch (SQLException e) {
            throw new AccesoDatosException("Error al comprobar el modelo", e);
        }
    }

    private Vehiculo mapearVehiculo(ResultSet rs) throws SQLException {
        Vehiculo vehiculo = new Vehiculo(
                rs.getString("placa"),
                rs.getInt("cliente_id"),
                rs.getInt("modelo_id")
        );
        vehiculo.setId(rs.getInt("id"));
        return vehiculo;
    }
}
