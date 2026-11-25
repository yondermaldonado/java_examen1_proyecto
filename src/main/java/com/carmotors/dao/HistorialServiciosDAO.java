package com.carmotors.dao;

import com.carmotors.modelo.ServicioHistorial;
import java.sql.Connection;
import java.util.List;

/** Consulta de historial; la conexión pertenece al servicio llamador. */
public interface HistorialServiciosDAO {
    List<ServicioHistorial> listarPorVehiculo(Connection conexion, int vehiculoId);
}
