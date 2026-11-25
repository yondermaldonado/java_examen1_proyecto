package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.EvaluacionProveedor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * SQL parametrizado para evaluaciones de proveedores.
 * Nunca hace commit/rollback ni cierra la Connection recibida.
 */
public class EvaluacionProveedorDAOJdbc implements EvaluacionProveedorDAO {

    private static final String COLUMNAS = """
            id, proveedor_id, lote_id, calificacion_calidad,
            calificacion_puntualidad, fecha_evaluacion, observaciones
            """;

    @Override
    public EvaluacionProveedor insertar(
            Connection conexion,
            EvaluacionProveedor evaluacion) {

        Objects.requireNonNull(conexion, "La conexion es obligatoria");
        Objects.requireNonNull(evaluacion, "La evaluacion es obligatoria");
        if (evaluacion.getId() != 0) {
            throw new IllegalArgumentException(
                    "No se puede insertar una evaluacion que ya tiene ID"
            );
        }

        String sql = """
                INSERT INTO evaluaciones_proveedor (
                    proveedor_id, lote_id, calificacion_calidad,
                    calificacion_puntualidad, fecha_evaluacion, observaciones
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conexion.prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, evaluacion.getProveedorId());
            ps.setInt(2, evaluacion.getLoteId());
            ps.setInt(3, evaluacion.getCalificacionCalidad());
            ps.setInt(4, evaluacion.getCalificacionPuntualidad());
            ps.setDate(5, Date.valueOf(evaluacion.getFechaEvaluacion()));
            if (evaluacion.getObservaciones() == null) {
                ps.setNull(6, Types.LONGVARCHAR);
            } else {
                ps.setString(6, evaluacion.getObservaciones());
            }

            if (ps.executeUpdate() != 1) {
                throw new AccesoDatosException("No se inserto la evaluacion");
            }

            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new AccesoDatosException(
                            "MySQL no devolvio el ID de la evaluacion"
                    );
                }

                return EvaluacionProveedor.desdePersistencia(
                        claves.getInt(1),
                        evaluacion.getProveedorId(),
                        evaluacion.getLoteId(),
                        evaluacion.getCalificacionCalidad(),
                        evaluacion.getCalificacionPuntualidad(),
                        evaluacion.getFechaEvaluacion(),
                        evaluacion.getObservaciones()
                );
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al registrar la evaluacion", e
            );
        }
    }

    @Override
    public Optional<EvaluacionProveedor> buscarPorId(Connection conexion, int id) {
        Objects.requireNonNull(conexion, "La conexion es obligatoria");
        validarId(id, "evaluacion");

        String sql = "SELECT " + COLUMNAS
                + " FROM evaluaciones_proveedor WHERE id = ?";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapear(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar la evaluacion", e
            );
        }
    }

    @Override
    public List<EvaluacionProveedor> listarPorProveedor(
            Connection conexion, int proveedorId) {
        Objects.requireNonNull(conexion, "La conexion es obligatoria");
        validarId(proveedorId, "proveedor");

        String sql = "SELECT " + COLUMNAS
                + " FROM evaluaciones_proveedor WHERE proveedor_id = ?"
                + " ORDER BY fecha_evaluacion DESC, id DESC";

        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, proveedorId);
            try (ResultSet rs = ps.executeQuery()) {
                List<EvaluacionProveedor> evaluaciones = new ArrayList<>();
                while (rs.next()) {
                    evaluaciones.add(mapear(rs));
                }
                return evaluaciones;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al listar evaluaciones del proveedor", e
            );
        }
    }

    @Override
    public boolean lotePerteneceAProveedor(
            Connection conexion, int loteId, int proveedorId) {
        Objects.requireNonNull(conexion, "La conexion es obligatoria");
        validarId(loteId, "lote");
        validarId(proveedorId, "proveedor");

        String sql = "SELECT 1 FROM lotes WHERE id = ? AND proveedor_id = ?";
        try (PreparedStatement ps = conexion.prepareStatement(sql)) {
            ps.setInt(1, loteId);
            ps.setInt(2, proveedorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al verificar el proveedor del lote", e
            );
        }
    }

    private EvaluacionProveedor mapear(ResultSet rs) throws SQLException {
        Date fecha = rs.getDate("fecha_evaluacion");
        if (fecha == null) {
            throw new SQLException("La evaluacion no tiene fecha");
        }
        return EvaluacionProveedor.desdePersistencia(
                rs.getInt("id"),
                rs.getInt("proveedor_id"),
                rs.getInt("lote_id"),
                rs.getInt("calificacion_calidad"),
                rs.getInt("calificacion_puntualidad"),
                fecha.toLocalDate(),
                rs.getString("observaciones")
        );
    }

    private static void validarId(int id, String entidad) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID de " + entidad + " debe ser mayor que cero"
            );
        }
    }
}
