package com.carmotors.servicio;

import com.carmotors.modelo.DatosOrdenParaFactura;
import java.util.Optional;

/** Provee el detalle histórico si la orden está en estado Completado y se puede facturar. */
@FunctionalInterface
public interface ConsultaOrdenCompletada {
    Optional<DatosOrdenParaFactura> buscar(long ordenId);
}
