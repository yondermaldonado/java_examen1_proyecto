package com.carmotors.dao;

import com.carmotors.modelo.DatosOrdenParaFactura;
import com.carmotors.modelo.FacturaDTO;
import com.carmotors.modelo.DocumentoFactura;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;

/** La conexión y la transacción de facturación pertenecen al servicio llamador. */
public interface FacturaDAO {
    Optional<DatosOrdenParaFactura> buscarOrdenCompletada(Connection conexion, long ordenId);
    Optional<DocumentoFactura> buscarPorOrden(Connection conexion, long ordenId);
    void insertar(Connection conexion, FacturaDTO factura, LocalDateTime fechaEmision);
}
