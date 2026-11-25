package com.carmotors.modelo;

import java.math.BigDecimal;

/** Precio histórico de la línea; clave lógica (orden, repuesto). */
public record LineaRepuestoOrden(int repuestoId, int cantidad, BigDecimal precioUnitario) { }
