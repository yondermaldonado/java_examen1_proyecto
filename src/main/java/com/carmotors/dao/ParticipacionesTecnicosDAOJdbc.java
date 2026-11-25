package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.FiltroReporte;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ParticipacionesTecnicosDAOJdbc implements ParticipacionesTecnicosDAO {
    private static final String CONSULTAR_PARTICIPACIONES = """
            SELECT tecnico.id, tecnico.nombre
            FROM servicios_tecnicos asignacion
            JOIN tecnicos tecnico ON tecnico.id = asignacion.tecnico_id
            JOIN servicios orden ON orden.id = asignacion.servicio_id
            JOIN servicio_estados estado ON estado.id = orden.estado_id
            WHERE estado.nombre IN ('Completado', 'Facturado', 'Entregado')
              AND orden.fecha_completado >= ? AND orden.fecha_completado <= ?
            """;
    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59, 59, 999_999_000);

    @Override
    public List<TecnicoParticipante> listarPorPeriodo(Connection conexion, FiltroReporte filtro) {
        try (PreparedStatement sentencia = conexion.prepareStatement(CONSULTAR_PARTICIPACIONES)) {
            sentencia.setObject(1, filtro.fechaInicio().atStartOfDay());
            sentencia.setObject(2, filtro.fechaFin().atTime(FIN_DEL_DIA));

            // La PK (servicio_id, tecnico_id) impide contar dos veces la misma asignación.
            List<TecnicoParticipante> participaciones = new ArrayList<>();
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    participaciones.add(new TecnicoParticipante(
                            resultados.getLong("id"), resultados.getString("nombre")));
                }
            }
            return participaciones;
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar las participaciones de los técnicos.", e);
        }
    }
}
