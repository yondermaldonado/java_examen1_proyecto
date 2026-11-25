package com.carmotors.dao;

import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.Lote;
import java.sql.Connection;
import java.util.List;

/** CONTRATO DAO: LoteService llama a estos metodos; LoteDAOJdbc ejecuta el SQL.
 * Acceso a datos de lotes. Recibe la conexión: no la cierra ni hace commit. */
public interface LoteDAO {
    /** Guarda el lote y todas sus líneas. Devuelve el id del lote. */
    int crear(Connection con, Lote lote);

    /** Líneas de un lote con su proveedor. Lista vacía si el código no existe. */
    List<ExistenciaLote> buscarPorCodigo(Connection con, String codigo);

    /** Todos los lotes donde hay (o hubo) un repuesto, con su proveedor. */
    List<ExistenciaLote> listarPorRepuesto(Connection con, int repuestoId);

    /** Todo lo que un proveedor ha entregado. */
    List<ExistenciaLote> listarPorProveedor(Connection con, int proveedorId);

    /** Suma de cantidad_actual de todos los lotes del repuesto. */
    int stockFisico(Connection con, int repuestoId);

    /** Suma de las reservas ACTIVAS del repuesto (solo lectura sobre reservas_repuestos). */
    int stockReservado(Connection con, int repuestoId);
}