package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.StockLote;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación MySQL del contrato ExistenciasDAO.
 * Esta es la única clase de este módulo que contiene SQL.
 *
 * Camino: ExistenciasService -> ExistenciasDAO -> ExistenciasDAOJdbc -> MySQL.
 */
public class ExistenciasDAOJdbc implements ExistenciasDAO {

    // PASO 1. Consultar la cantidad física de cada lote-repuesto.
    // PASO 2. Sumar únicamente reservas cuyo estado sea ACTIVA.
    // PASO 3. LEFT JOIN: un lote sin reservas también debe aparecer.
    // Se conserva la misma consulta SQL del proyecto para no cambiar los cálculos.
    private static final String CONSULTA_BASE =
            "SELECT lr.lote_id, l.codigo, lr.repuesto_id, lr.fecha_caducidad, "
            + "lr.cantidad_actual AS fisico, COALESCE(ra.total, 0) AS reservado "
            + "FROM lotes_repuestos lr JOIN lotes l ON l.id = lr.lote_id "
            + "LEFT JOIN (SELECT rr.lote_id, rr.repuesto_id, SUM(rr.cantidad) AS total "
            + "FROM reservas_repuestos rr JOIN reserva_estados re ON re.id = rr.estado_id "
            + "WHERE re.nombre = 'ACTIVA' GROUP BY rr.lote_id, rr.repuesto_id) ra "
            + "ON ra.lote_id = lr.lote_id AND ra.repuesto_id = lr.repuesto_id ";

    // Lo llama ExistenciasService.porRepuesto().
    @Override
    public List<StockLote> porRepuesto(Connection conexion, int repuestoId) {
        String sql = CONSULTA_BASE + "WHERE lr.repuesto_id = ? ORDER BY lr.lote_id";
        return consultar(conexion, sql, repuestoId, null);
    }

    // Lo llama ExistenciasService.porLote().
    @Override
    public List<StockLote> porLote(Connection conexion, int loteId, int repuestoId) {
        String sql = CONSULTA_BASE + "WHERE lr.lote_id = ? AND lr.repuesto_id = ?";
        return consultar(conexion, sql, loteId, repuestoId);
    }

    // Método común: ejecuta el SELECT y convierte cada fila en StockLote.
    // La conexión llega del Service y el DAO NO la cierra.
    private List<StockLote> consultar(Connection conexion, String sql,
                                     int primerParametro, Integer segundoParametro) {
        List<StockLote> resultado = new ArrayList<>();

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            // PASO 1. Colocar los parámetros para evitar concatenar IDs al SQL.
            consulta.setInt(1, primerParametro);
            if (segundoParametro != null) {
                consulta.setInt(2, segundoParametro);
            }

            // PASO 2. Ejecutar la consulta.
            try (ResultSet filas = consulta.executeQuery()) {
                // PASO 3. Leer cada fila y convertirla en un objeto StockLote.
                while (filas.next()) {
                    int loteId = filas.getInt("lote_id");
                    String codigo = filas.getString("codigo");
                    int repuestoId = filas.getInt("repuesto_id");
                    long fisico = filas.getLong("fisico");
                    long reservado = filas.getLong("reservado");

                    Date fechaSQL = filas.getDate("fecha_caducidad");
                    LocalDate caducidad = null;
                    if (fechaSQL != null) {
                        caducidad = fechaSQL.toLocalDate();
                    }

                    StockLote stock = new StockLote(
                            loteId, codigo, repuestoId, caducidad, fisico, reservado);
                    resultado.add(stock);
                }
            }
            // PASO 4. PreparedStatement y ResultSet se cierran automáticamente.
            return resultado;
        } catch (SQLException error) {
            throw new DaoException("No se pudieron consultar las existencias.", error);
        }
    }
}
