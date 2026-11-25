package com.carmotors.servicio;

import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.LineaRepuestoOrden;
import com.carmotors.modelo.ReservaOrden;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Puerto de persistencia: lecturas actuales/bloqueos y escrituras en la Connection recibida. */
public interface InventarioReservas {
    Optional<BigDecimal> bloquearPrecio(Connection con, int repuestoId);
    List<LineaRepuestoOrden> bloquearLineas(Connection con, int ordenId);
    List<ExistenciaLote> bloquearLotesDisponibles(Connection con, int repuestoId);
    List<ReservaOrden> bloquearReservas(Connection con, int ordenId, int repuestoId);
    void guardarLinea(Connection con, int ordenId, LineaRepuestoOrden linea);
    void liberarActivas(Connection con, int ordenId, int repuestoId);
    void reservar(Connection con, int ordenId, AsignacionLote lote, LocalDateTime fecha);
    boolean descontarLote(Connection con, int loteId, int repuestoId, int cantidad);
    boolean consumirReserva(Connection con, int reservaId, LocalDateTime fecha);
}
