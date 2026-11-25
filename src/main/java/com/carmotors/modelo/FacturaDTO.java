package com.carmotors.modelo;

import java.math.BigDecimal;

/** Detalle y totales calculados para una orden completada. */
public record FacturaDTO(long ordenId, DatosOrdenParaFactura detalle, BigDecimal totalRepuestos,
        BigDecimal totalManoObra, BigDecimal tasaImpuesto, BigDecimal impuestos, BigDecimal totalFinal) {
}
