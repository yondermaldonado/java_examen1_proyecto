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

public class ConsumoRepuestosDAOJdbc implements ConsumoRepuestosDAO {
    private static final String CONSULTAR_CONSUMOS = """
            SELECT repuesto.id, repuesto.nombre, reserva.cantidad
            FROM reservas_repuestos reserva
            JOIN reserva_estados estado ON estado.id = reserva.estado_id
            JOIN repuestos repuesto ON repuesto.id = reserva.repuesto_id
            WHERE estado.nombre = 'CONSUMIDA'
              AND reserva.fecha_consumo >= ? AND reserva.fecha_consumo <= ?
            """;
    private static final LocalTime FIN_DEL_DIA = LocalTime.of(23, 59, 59, 999_999_000);

    @Override
    public List<Consumo> listarPorPeriodo(Connection conexion, FiltroReporte filtro) {
        try (PreparedStatement sentencia = conexion.prepareStatement(CONSULTAR_CONSUMOS)) {
            sentencia.setObject(1, filtro.fechaInicio().atStartOfDay());
            sentencia.setObject(2, filtro.fechaFin().atTime(FIN_DEL_DIA));

            List<Consumo> consumos = new ArrayList<>();
            try (ResultSet resultados = sentencia.executeQuery()) {
                while (resultados.next()) {
                    var repuesto = new RepuestoConsumido(resultados.getLong("id"),
                            resultados.getString("nombre"));
                    consumos.add(new Consumo(repuesto, resultados.getLong("cantidad")));
                }
            }
            return consumos;
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar el consumo de repuestos.", e);
        }
    }
}
