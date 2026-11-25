package com.carmotors.modelo;

import java.time.LocalDateTime;

/** Fila de reserva bloqueada para una operación transaccional. */
public record ReservaOrden(int id, int repuestoId, int loteId, int cantidad,
                           String estado, LocalDateTime fechaReserva) { }
