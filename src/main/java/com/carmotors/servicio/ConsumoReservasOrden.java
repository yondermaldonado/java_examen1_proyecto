package com.carmotors.servicio;

import java.sql.Connection;
import java.util.Set;

/** Consume reservas/stock una vez en la conexión recibida, sin commit/cierre/eventos externos. */
@FunctionalInterface
public interface ConsumoReservasOrden {
    /** Devuelve los IDs afectados para notificar solo después del commit del servicio. */
    Set<Integer> consumir(Connection conexion, int ordenId);
}
