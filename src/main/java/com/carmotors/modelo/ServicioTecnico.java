package com.carmotors.modelo;

import java.math.BigDecimal;

/** La clave es (servicioId, tecnicoId); nombres/estado son datos de consulta. */
public record ServicioTecnico(int servicioId, int tecnicoId, String nombreTecnico,
                              String estadoOrden, BigDecimal horasTrabajadas) { }
