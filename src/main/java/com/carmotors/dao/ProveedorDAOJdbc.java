
package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.Proveedor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProveedorDAOJdbc implements ProveedorDAO {

    private static final String COLUMNAS =
            "id, nombre, contacto, email";

    @Override
    public Proveedor insertar(
            Connection conexion, Proveedor proveedor) {

        if (proveedor.getId() != 0) {
            throw new IllegalArgumentException(
                    "El proveedor ya tiene un ID"
            );
        }

        String sql = """
                INSERT INTO proveedores (nombre, contacto, email)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, proveedor.getNombre());
            ps.setString(2, proveedor.getContacto());
            ps.setString(3, proveedor.getEmail());

            if (ps.executeUpdate() != 1) {
                throw new AccesoDatosException(
                        "No se pudo insertar el proveedor"
                );
            }

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new AccesoDatosException(
                            "No se obtuvo el ID generado"
                    );
                }

                proveedor.setId(claves.getInt(1));
                return proveedor;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al registrar el proveedor", e
            );
        }
    }

    @Override
    public Optional<Proveedor> buscarPorId(
            Connection conexion, int id) {

        String sql = "SELECT " + COLUMNAS
                + " FROM proveedores WHERE id = ?";

        List<Proveedor> resultados =
                consultar(conexion, sql, id);

        return resultados.stream().findFirst();
    }

    @Override
    public List<Proveedor> buscarPorNombre(
            Connection conexion, String nombre) {

        String sql = "SELECT " + COLUMNAS
                + " FROM proveedores "
                + "WHERE nombre LIKE ? ORDER BY nombre, id";

        return consultar(conexion, sql, "%" + nombre + "%");
    }

    @Override
    public List<Proveedor> buscarPorEmail(
            Connection conexion, String email) {

        String sql = "SELECT " + COLUMNAS
                + " FROM proveedores "
                + "WHERE email = ? ORDER BY id";

        return consultar(conexion, sql, email);
    }

    @Override
    public List<Proveedor> listar(Connection conexion) {

        String sql = "SELECT " + COLUMNAS
                + " FROM proveedores ORDER BY id";

        return consultar(conexion, sql);
    }

    @Override
    public boolean existe(Connection conexion, int id) {

        String sql = "SELECT 1 FROM proveedores WHERE id = ?";

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al verificar el proveedor", e
            );
        }
    }

    @Override
    public boolean actualizar(
            Connection conexion, Proveedor proveedor) {

        String sql = """
                UPDATE proveedores
                SET nombre = ?, contacto = ?, email = ?
                WHERE id = ?
                """;

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setString(1, proveedor.getNombre());
            ps.setString(2, proveedor.getContacto());
            ps.setString(3, proveedor.getEmail());
            ps.setInt(4, proveedor.getId());

            int filas = ps.executeUpdate();

            if (filas > 0) {
                return true;
            }

            // MySQL puede devolver 0 si los datos no cambiaron.
            return existe(conexion, proveedor.getId());

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al actualizar el proveedor", e
            );
        }
    }

    @Override
    public boolean eliminar(Connection conexion, int id) {

        String sql = "DELETE FROM proveedores WHERE id = ?";

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            // Conserva el error de FK para que el servicio
            // pueda explicar por que no se permite eliminar.
            throw new AccesoDatosException(
                    "Error al eliminar el proveedor", e
            );
        }
    }

    private List<Proveedor> consultar(
            Connection conexion,
            String sql,
            Object... parametros) {

        List<Proveedor> proveedores = new ArrayList<>();

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    proveedores.add(mapearProveedor(rs));
                }
            }

            return proveedores;

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al consultar proveedores", e
            );
        }
    }

    private Proveedor mapearProveedor(ResultSet rs)
            throws SQLException {

        Proveedor proveedor = new Proveedor(
                rs.getString("nombre"),
                rs.getString("contacto"),
                rs.getString("email")
        );

        proveedor.setId(rs.getInt("id"));
        return proveedor;
    }
}
