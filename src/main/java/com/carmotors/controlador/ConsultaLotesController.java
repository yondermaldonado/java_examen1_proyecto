package com.carmotors.controlador;

import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.servicio.LoteService;

import java.util.List;
import java.util.Objects;

/** Consulta tipada para seleccionar lotes sin analizar cadenas formateadas. */
public final class ConsultaLotesController {
    private final LoteService servicio;

    public ConsultaLotesController(LoteService servicio) {
        this.servicio = Objects.requireNonNull(servicio, "El servicio de lotes es obligatorio");
    }

    /** Cada fila describe un repuesto del lote; la vista agrupa por loteId. */
    public List<ExistenciaLote> listarPorProveedor(int proveedorId) {
        return servicio.listarPorProveedor(proveedorId);
    }
}
