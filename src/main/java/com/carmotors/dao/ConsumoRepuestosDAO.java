package com.carmotors.dao;

import com.carmotors.modelo.FiltroReporte;
import java.sql.Connection;
import java.util.List;

/** Consulta de solo lectura de reservas efectivamente consumidas. */
public interface ConsumoRepuestosDAO {
    List<Consumo> listarPorPeriodo(Connection conexion, FiltroReporte filtro);

    record RepuestoConsumido(long id, String nombre) {
    }

    record Consumo(RepuestoConsumido repuesto, long unidades) {
    }
}
