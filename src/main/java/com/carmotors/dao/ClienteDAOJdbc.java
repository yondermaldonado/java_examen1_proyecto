package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.Cliente;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAOJdbc implements ClienteDAO {

    private static final String COLUMNAS =
            "id, nombre, documento_identidad, telefono, email";

    // CREATE
    @Override
    public Cliente insertar(Connection conexion, Cliente cliente) {
        if (cliente.getId() != 0) {
            throw new IllegalArgumentException(
                    "El cliente ya tiene un ID asignado");
        }

        String sql = """
                INSERT INTO clientes
                (nombre, documento_identidad, telefono, email)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDocumento());
            ps.setString(3, cliente.getTelefono());
            asignarEmail(ps, 4, cliente.getEmail());

            int filas = ps.executeUpdate();
            if (filas != 1) {
                throw new AccesoDatosException(
                        "No se pudo insertar el cliente");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new AccesoDatosException(
                            "No se obtuvo el ID del cliente registrado");
                }
                // En el DAO este ID todavía es provisional:
                // quien gestione la conexión decidirá el commit o rollback.
                cliente.setId(rs.getInt(1));
                return cliente;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al insertar el cliente", e);
        }
    }

    // READ: buscar por ID
    @Override
    public Optional<Cliente> buscarPorId(Connection conexion, int id) {
        String sql = "SELECT " + COLUMNAS + " FROM clientes WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar cliente por ID", e);
        }
    }

    // READ: buscar por documento
    @Override
    public Optional<Cliente> buscarPorDocumento(
            Connection conexion, String documento) {

        String sql = "SELECT " + COLUMNAS
                + " FROM clientes WHERE documento_identidad = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar cliente por documento", e);
        }
    }

    // READ: buscar por email
    @Override
    public Optional<Cliente> buscarPorEmail(Connection conexion, String email) {
        String sql = "SELECT " + COLUMNAS + " FROM clientes WHERE email = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapearCliente(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar cliente por email", e);
        }
    }

    // READ: listar todos
    @Override
    public List<Cliente> listar(Connection conexion) {
        String sql = "SELECT " + COLUMNAS + " FROM clientes ORDER BY id";
        List<Cliente> clientes = new ArrayList<>();

        try (PreparedStatement ps = conexion.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }
            return clientes;

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al listar clientes", e);
        }
    }

    // UPDATE
    @Override
    public boolean actualizar(Connection conexion, Cliente cliente) {
        String sql = """
                UPDATE clientes
                SET nombre = ?,
                    documento_identidad = ?,
                    telefono = ?,
                    email = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getDocumento());
            ps.setString(3, cliente.getTelefono());
            asignarEmail(ps, 4, cliente.getEmail());
            ps.setInt(5, cliente.getId());

            int filas = ps.executeUpdate();
            if (filas > 0) {
                return true;
            }

            // Una actualización sin modificaciones puede devolver 0.
            // Distinguimos ese caso de un ID inexistente.
            return buscarPorId(conexion, cliente.getId()).isPresent();

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al actualizar cliente", e);
        }
    }

    // DELETE
    @Override
    public boolean eliminar(Connection conexion, int id) {
        String sql = "DELETE FROM clientes WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            // Conservamos la causa SQL original. No asumimos que
            // todos los errores de clave foránea son por vehículos.
            throw new AccesoDatosException(
                    "Error al eliminar cliente", e);
        }
    }

    // AUXILIARES
    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente(
                rs.getString("nombre"),
                rs.getString("documento_identidad"),
                rs.getString("telefono"),
                rs.getString("email")
        );
        cliente.setId(rs.getInt("id"));
        return cliente;
    }

    private void asignarEmail(
            PreparedStatement ps, int posicion, String email) throws SQLException {

        if (email == null) {
            ps.setNull(posicion, Types.VARCHAR);
        } else {
            ps.setString(posicion, email);
        }
    }
}
