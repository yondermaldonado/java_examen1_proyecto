package com.carmotors.dao;

import com.carmotors.modelo.StockLote;
import java.sql.Connection;
import java.util.List;

/**
 * Contrato de consultas de inventario (principio DIP de SOLID).
 * ExistenciasService conoce esta interfaz, no una implementación concreta.
 * Solo consulta información; no hace reservas, retiros ni commit.
 */
public interface ExistenciasDAO {

    // Devuelve todos los lotes asociados a un repuesto, con reservas ACTIVAS.
    List<StockLote> porRepuesto(Connection conexion, int repuestoId);

    // Devuelve la fila de un lote y repuesto concreto. Lista vacía si no existe.
    List<StockLote> porLote(Connection conexion, int loteId, int repuestoId);
}
