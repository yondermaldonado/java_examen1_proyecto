package com.carmotors.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Cabecera de cliente/vehículo, detalle histórico y totales ya persistidos. */
public record DocumentoFactura(long id, long ordenId, LocalDateTime fechaEmision,
        String cliente, String documento, String telefono, String email,
        String placa, String marca, String modelo, DatosOrdenParaFactura detalle,
        BigDecimal totalRepuestos, BigDecimal totalManoObra, BigDecimal impuestos, BigDecimal totalFinal) {}
