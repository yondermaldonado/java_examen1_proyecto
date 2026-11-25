
package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ProveedorRepuesto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProveedorRepuestoDAOJdbc
        implements ProveedorRepuestoDAO {

    private static final String COLUMNAS =
            "proveedor_id, repuesto_id, costo";

    // =========================================
    // CREATE - REGISTRAR ASOCIACION
    // =========================================

    @Override
    public void insertar(
            Connection conexion,
            ProveedorRepuesto asociacion) {

        Objects.requireNonNull(
                asociacion,
                "La asociacion no puede ser nula"
        );

        String sql = """
                INSERT INTO proveedores_repuestos
                    (proveedor_id, repuesto_id, costo)
                VALUES (?, ?, ?)
                """;

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, asociacion.getProveedorId());
            ps.setInt(2, asociacion.getRepuestoId());
            ps.setBigDecimal(3, asociacion.getCosto());

            int filas = ps.executeUpdate();

            if (filas != 1) {
                throw new AccesoDatosException(
                        "No se pudo registrar la asociacion"
                );
            }

        } catch (SQLException e) {
            throw traducir(
                    e,
                    "Error al registrar la asociacion"
            );
        }
    }

    // =========================================
    // READ - BUSCAR ASOCIACION EXACTA
    // =========================================

    @Override
    public Optional<ProveedorRepuesto> buscar(
            Connection conexion,
            int proveedorId,
            int repuestoId) {

        validarId(proveedorId, "proveedor");
        validarId(repuestoId, "repuesto");

        String sql = """
                SELECT proveedor_id, repuesto_id, costo
                FROM proveedores_repuestos
                WHERE proveedor_id = ?
                  AND repuesto_id = ?
                """;

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, proveedorId);
            ps.setInt(2, repuestoId);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }

                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar la asociacion",
                    e
            );
        }
    }

    // =========================================
    // READ - REPUESTOS DE UN PROVEEDOR
    // =========================================

    @Override
    public List<ProveedorRepuesto> listarPorProveedor(
            Connection conexion,
            int proveedorId) {

        validarId(proveedorId, "proveedor");

        String sql = """
                SELECT proveedor_id, repuesto_id, costo
                FROM proveedores_repuestos
                WHERE proveedor_id = ?
                ORDER BY repuesto_id
                """;

        return consultar(conexion, sql, proveedorId);
    }

    // =========================================
    // READ - PROVEEDORES DE UN REPUESTO
    // =========================================

    @Override
    public List<ProveedorRepuesto> listarPorRepuesto(
            Connection conexion,
            int repuestoId) {

        validarId(repuestoId, "repuesto");

        String sql = """
                SELECT proveedor_id, repuesto_id, costo
                FROM proveedores_repuestos
                WHERE repuesto_id = ?
                ORDER BY costo, proveedor_id
                """;

        return consultar(conexion, sql, repuestoId);
    }

    // =========================================
    // UPDATE - ACTUALIZAR SOLO EL COSTO
    // =========================================

    @Override
    public boolean actualizarCosto(
            Connection conexion,
            ProveedorRepuesto asociacion) {

        Objects.requireNonNull(
                asociacion,
                "La asociacion no puede ser nula"
        );

        String sql = """
                UPDATE proveedores_repuestos
                SET costo = ?
                WHERE proveedor_id = ?
                  AND repuesto_id = ?
                """;

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setBigDecimal(
                    1,
                    asociacion.getCosto()
            );

            ps.setInt(
                    2,
                    asociacion.getProveedorId()
            );

            ps.setInt(
                    3,
                    asociacion.getRepuestoId()
            );

            int filas = ps.executeUpdate();

            if (filas > 0) {
                return true;
            }

            // MySQL puede devolver 0 cuando
            // el costo ya tenia el mismo valor.
            return buscar(
                    conexion,
                    asociacion.getProveedorId(),
                    asociacion.getRepuestoId()
            ).isPresent();

        } catch (SQLException e) {
            throw traducir(
                    e,
                    "Error al actualizar el costo"
            );
        }
    }

    // =========================================
    // CONSULTAS REUTILIZABLES
    // =========================================

    private List<ProveedorRepuesto> consultar(
            Connection conexion,
            String sql,
            int parametro) {

        List<ProveedorRepuesto> resultados =
                new ArrayList<>();

        try (PreparedStatement ps =
                     conexion.prepareStatement(sql)) {

            ps.setInt(1, parametro);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    resultados.add(mapear(rs));
                }
            }

            return resultados;

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al consultar el catalogo",
                    e
            );
        }
    }

    // =========================================
    // CONVERTIR FILA SQL EN OBJETO JAVA
    // =========================================

    private ProveedorRepuesto mapear(ResultSet rs)
            throws SQLException {

        return new ProveedorRepuesto(
                rs.getInt("proveedor_id"),
                rs.getInt("repuesto_id"),
                rs.getBigDecimal("costo")
        );
    }

    // =========================================
    // VALIDACIONES BASICAS
    // =========================================

    private void validarId(int id, String entidad) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID del " + entidad
                            + " debe ser mayor que cero"
            );
        }
    }

    // =========================================
    // TRADUCIR ERRORES MYSQL
    // =========================================

    private RuntimeException traducir(
            SQLException e,
            String mensaje) {

        return switch (e.getErrorCode()) {

            case 1062 -> new ValidacionException(
                    "La asociacion entre este proveedor "
                            + "y repuesto ya existe"
            );

            case 1452 -> new ValidacionException(
                    "El proveedor o el repuesto "
                            + "indicado no existe"
            );

            case 3819 -> new ValidacionException(
                    "El costo incumple una restriccion "
                            + "de la base de datos"
            );

            case 1264 -> new ValidacionException(
                    "El costo esta fuera del rango permitido"
            );

            default -> new AccesoDatosException(
                    mensaje,
                    e
            );
        };
    }
}
