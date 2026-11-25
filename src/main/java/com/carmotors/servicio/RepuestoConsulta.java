
package com.carmotors.servicio;

/**
 * Contrato mínimo para consultar la existencia
 * de un repuesto sin depender de su implementación.
 */
@FunctionalInterface
public interface RepuestoConsulta {

    boolean existe(int repuestoId);
}
