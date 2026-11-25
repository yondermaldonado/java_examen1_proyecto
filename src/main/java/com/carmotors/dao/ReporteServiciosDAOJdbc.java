package com.carmotors.dao;

import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.SolicitudServicioReporteDato;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ReporteServiciosDAOJdbc implements ReporteServiciosDAO {
    private static final String CONSULTAR_SOLICITUDES = """
            SELECT orden.id, tipo.nombre, orden.fecha_recepcion
            FROM servicios orden
            JOIN mantenimiento_tipos tipo ON tipo.id = orden.mantenimiento_tipo_id
            WHERE orden.fecha_recepcion >= ? AND orden.fecha_recepcion <= ?
            """;

    private static final String CONSULTAR_COMPLETADOS = """
        SELECT orden.id,
               tipo.nombre AS tipo,
               estado.nombre AS estado,
               orden.fecha_recepcion,
               orden.fecha_completado,
               orden.fecha_entrega
        FROM servicios orden
        JOIN mantenimiento_tipos tipo
            ON tipo.id = orden.mantenimiento_tipo_id
        JOIN servicio_estados estado
            ON estado.id = orden.estado_id
        WHERE orden.fecha_completado IS NOT NULL
          AND orden.fecha_completado >= ?
          AND orden.fecha_completado <= ?
          AND estado.nombre = 'Completado'
        ORDER BY orden.fecha_completado ASC
        """;
    // MySQL admite hasta seis dígitos de precisión en DATETIME.
    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59, 59, 999_999_000);

    @Override
    public List<SolicitudServicioReporteDato> listarSolicitudes(Connection conexion, FiltroReporte filtro) {
        try (PreparedStatement sentencia = conexion.prepareStatement(CONSULTAR_SOLICITUDES)) {
            sentencia.setObject(1, filtro.fechaInicio().atStartOfDay());
            sentencia.setObject(2, filtro.fechaFin().atTime(FIN_DEL_DIA));

            List<SolicitudServicioReporteDato> solicitudes = new ArrayList<>();
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    solicitudes.add(new SolicitudServicioReporteDato(
                            resultados.getLong("id"),
                            resultados.getString("nombre"),
                            resultados.getObject("fecha_recepcion", LocalDateTime.class).toLocalDate()));
                }
            }
            return solicitudes;
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar el reporte de servicios más solicitados.", e);
        }
    }
    @Override
    public List<FilaReporteDTO> listarCompletados(
            Connection conexion,
            FiltroReporte filtro
    ) {
        try (PreparedStatement sentencia =
                     conexion.prepareStatement(CONSULTAR_COMPLETADOS)) {

            sentencia.setObject(
                    1, filtro.fechaInicio().atStartOfDay()
            );

            sentencia.setObject(
                    2, filtro.fechaFin().atTime(FIN_DEL_DIA)
            );

            List<FilaReporteDTO> servicios = new ArrayList<>();

            try (ResultSet resultados = sentencia.executeQuery()) {

                while (resultados.next()) {

                    servicios.add(new FilaReporteDTO(
                            resultados.getString("id"),
                            resultados.getString("tipo"),
                            1,
                            resultados.getObject(
                                    "fecha_recepcion",
                                    LocalDateTime.class
                            ),
                            resultados.getObject(
                                    "fecha_completado",
                                    LocalDateTime.class
                            ),
                            resultados.getObject(
                                    "fecha_entrega",
                                    LocalDateTime.class
                            ),
                            resultados.getString("estado")
                    ));
                }
            }

            return servicios;

        } catch (SQLException e) {
            throw new DaoException(
                    "No se pudo consultar los servicios completados.",
                    e
            );
        }
    }
}
