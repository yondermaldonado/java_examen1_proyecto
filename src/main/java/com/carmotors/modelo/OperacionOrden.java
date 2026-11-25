package com.carmotors.modelo;

import java.math.BigDecimal;

/** Precio acordado para una operación de una orden, independiente del precio actual del catálogo. */
public record OperacionOrden(int catalogoId, String nombre, BigDecimal precioCobrado) {}
