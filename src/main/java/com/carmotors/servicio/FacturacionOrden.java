package com.carmotors.servicio;

import java.sql.Connection;
import java.time.LocalDateTime;

/** Persiste una factura única sin cambiar el estado, confirmar ni cerrar la conexión. */
@FunctionalInterface
public interface FacturacionOrden {
    void facturar(Connection conexion, int ordenId, LocalDateTime fechaEmision);
}
