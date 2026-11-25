package com.carmotors.dao;

import com.carmotors.modelo.FiltroReporte;
import java.sql.Connection;
import java.util.List;

/** Consulta de solo lectura para el reporte de productividad. */
public interface ParticipacionesTecnicosDAO {
    /** Devuelve una entrada por cada par orden completada y técnico asignado en el periodo. */
    List<TecnicoParticipante> listarPorPeriodo(Connection conexion, FiltroReporte filtro);

    record TecnicoParticipante(long id, String nombre) {
    }
}
