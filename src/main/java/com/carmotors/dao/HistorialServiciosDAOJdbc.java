
package com.carmotors.dao;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.OperacionHistorial;
import com.carmotors.modelo.ServicioHistorial;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HistorialServiciosDAOJdbc implements HistorialServiciosDAO {

    private static final String SQL = """
            SELECT
                s.id AS servicio_id,
                s.vehiculo_id,
                s.fecha_recepcion,
                s.fecha_entrega,
                s.descripcion_problema,
                e.nombre AS estado,
                t.nombre AS tipo_mantenimiento,
                d.servicio_catalogo_id AS operacion_id,
                c.nombre AS nombre_operacion,
                d.precio_cobrado,
                c.intervalo_meses
            FROM servicios s
            INNER JOIN servicio_estados e
                ON e.id = s.estado_id
            INNER JOIN mantenimiento_tipos t
                ON t.id = s.mantenimiento_tipo_id
            LEFT JOIN servicios_detalle d
                ON d.servicio_id = s.id
            LEFT JOIN servicios_catalogo c
                ON c.id = d.servicio_catalogo_id
            WHERE s.vehiculo_id = ?
            ORDER BY
                s.fecha_recepcion DESC,
                s.id DESC,
                d.servicio_catalogo_id
            """;

    @Override
    public List<ServicioHistorial> listarPorVehiculo(
            Connection conexion,
            int vehiculoId) {

        Objects.requireNonNull(
                conexion, "La conexion es obligatoria");

        if (vehiculoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del vehiculo debe ser positivo");
        }

        Map<Integer, ServicioEnConstruccion> servicios =
                new LinkedHashMap<>();

        try (PreparedStatement ps =
                     conexion.prepareStatement(SQL)) {

            ps.setInt(1, vehiculoId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    int servicioId =
                            rs.getInt("servicio_id");

                    ServicioEnConstruccion registro =
                            servicios.get(servicioId);

                    if (registro == null) {
                        registro = new ServicioEnConstruccion(rs);
                        servicios.put(servicioId, registro);
                    }

                    registro.agregarOperacion(rs);
                }
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al consultar historial de servicios", e);
        }

        List<ServicioHistorial> resultado = new ArrayList<>();

        for (ServicioEnConstruccion registro
                : servicios.values()) {

            resultado.add(registro.construir());
        }

        return resultado;
    }

    // Agrupa las operaciones pertenecientes a un mismo servicio.
    private static class ServicioEnConstruccion {

        private final int servicioId;
        private final int vehiculoId;
        private final LocalDateTime fechaRecepcion;
        private final LocalDateTime fechaEntrega;
        private final String estado;
        private final String tipoMantenimiento;
        private final String descripcionProblema;

        private final List<OperacionHistorial> operaciones =
                new ArrayList<>();

        private ServicioEnConstruccion(ResultSet rs)
                throws SQLException {

            servicioId = rs.getInt("servicio_id");
            vehiculoId = rs.getInt("vehiculo_id");

            fechaRecepcion = rs.getTimestamp(
                    "fecha_recepcion").toLocalDateTime();

            Timestamp entrega =
                    rs.getTimestamp("fecha_entrega");

            fechaEntrega = entrega == null
                    ? null
                    : entrega.toLocalDateTime();

            estado = rs.getString("estado");

            tipoMantenimiento =
                    rs.getString("tipo_mantenimiento");

            descripcionProblema =
                    rs.getString("descripcion_problema");
        }

        private void agregarOperacion(ResultSet rs)
                throws SQLException {

            Integer operacionId =
                    rs.getObject("operacion_id", Integer.class);

            // Una orden puede existir sin operaciones asociadas.
            if (operacionId == null) {
                return;
            }

            Integer intervalo =
                    rs.getObject("intervalo_meses", Integer.class);

            OperacionHistorial operacion =
                    new OperacionHistorial(
                            operacionId,
                            rs.getString("nombre_operacion"),
                            rs.getBigDecimal("precio_cobrado"),
                            intervalo
                    );

            operaciones.add(operacion);
        }

        private ServicioHistorial construir() {

            return new ServicioHistorial(
                    servicioId,
                    vehiculoId,
                    fechaRecepcion,
                    fechaEntrega,
                    estado,
                    tipoMantenimiento,
                    descripcionProblema,
                    operaciones
            );
        }
    }
}
