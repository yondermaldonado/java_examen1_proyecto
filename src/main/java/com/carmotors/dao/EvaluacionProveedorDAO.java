package com.carmotors.dao;

import com.carmotors.modelo.EvaluacionProveedor;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia. El servicio controla las conexiones y transacciones.
 */
public interface EvaluacionProveedorDAO {

    EvaluacionProveedor insertar(
            Connection conexion,
            EvaluacionProveedor evaluacion
    );

    Optional<EvaluacionProveedor> buscarPorId(Connection conexion, int id);

    List<EvaluacionProveedor> listarPorProveedor(
            Connection conexion,
            int proveedorId
    );

    /** Consulta estricta de la relación entre el lote y su proveedor. */
    boolean lotePerteneceAProveedor(
            Connection conexion,
            int loteId,
            int proveedorId
    );
}
