package com.carmotors.servicio;

import com.carmotors.dao.OrdenServicioDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.LineaRepuestoOrden;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.ReservaOrden;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Reserva/ajuste en transacciones propias; consumo en la transacción de la orden. */
public class ReservasOrdenService implements ConsumoReservasOrden {
    private final OrdenServicioDAO ordenes;
    private final InventarioReservas inventario;
    private final Supplier<Connection> conexiones;
    private final Clock reloj;
    private final PublicadorInventario publicador;

    public ReservasOrdenService(OrdenServicioDAO ordenes, InventarioReservas inventario,
                                Supplier<Connection> conexiones, Clock reloj, PublicadorInventario publicador) {
        this.ordenes=Objects.requireNonNull(ordenes);
        this.inventario=Objects.requireNonNull(inventario);
        this.conexiones=Objects.requireNonNull(conexiones);
        this.reloj=Objects.requireNonNull(reloj);
        this.publicador=Objects.requireNonNull(publicador);
    }

    public void reservar(int orden,int repuesto,int cantidad) { preparar(orden,repuesto,cantidad,false); }
    /** Cantidad total deseada, no un incremento; conserva el precio histórico. */
    public void ajustar(int orden,int repuesto,int cantidadTotal) { preparar(orden,repuesto,cantidadTotal,true); }

    private void preparar(int orden,int repuesto,int cantidad,boolean ajuste) {
        validarIds(orden,repuesto);
        if(cantidad<=0) throw new ValidacionException("La cantidad debe ser mayor que cero.");
        transaccion(con->{
            abierta(con,orden);
            BigDecimal actual=precio(con,repuesto);
            LineaRepuestoOrden previa=inventario.bloquearLineas(con,orden).stream()
                    .filter(l->l.repuestoId()==repuesto).findFirst().orElse(null);
            if(ajuste && previa==null) throw new ValidacionException("No existe la línea; utiliza reservar primero.");
            if(!ajuste && previa!=null) throw new ValidacionException("El repuesto ya tiene una línea; utiliza ajustar.");
            sinConsumo(inventario.bloquearReservas(con,orden,repuesto));
            // Primero libera para recalcular disponible. Si FEFO falla, el rollback restaura todo.
            inventario.liberarActivas(con,orden,repuesto);
            List<ExistenciaLote> lotes=inventario.bloquearLotesDisponibles(con,repuesto);
            List<AsignacionLote> plan=PoliticaFefo.asignar(lotes,cantidad,LocalDate.now(reloj));
            BigDecimal historico=previa==null?actual:previa.precioUnitario();
            inventario.guardarLinea(con,orden,new LineaRepuestoOrden(repuesto,cantidad,historico));
            LocalDateTime fecha=LocalDateTime.now(reloj).withNano(0);
            for(AsignacionLote lote:plan) inventario.reservar(con,orden,lote,fecha);
        });
        publicador.publicar(repuesto);
    }

    /** Libera disponibilidad, conservando la línea pendiente y el historial LIBERADA. */
    public void liberar(int orden,int repuesto) {
        validarIds(orden,repuesto);
        transaccion(con->{
            abierta(con,orden);precio(con,repuesto);
            if(inventario.bloquearLineas(con,orden).stream().noneMatch(l->l.repuestoId()==repuesto)) {
                throw new ValidacionException("No existe esa línea de la orden.");
            }
            List<ReservaOrden> reservas=inventario.bloquearReservas(con,orden,repuesto);
            sinConsumo(reservas);
            if(reservas.stream().noneMatch(r->r.estado().equals("ACTIVA"))) {
                throw new ValidacionException("La línea no tiene reservas activas.");
            }
            inventario.liberarActivas(con,orden,repuesto);
        });
        publicador.publicar(repuesto);
    }

    @Override
    public Set<Integer> consumir(Connection con,int orden) {
        if(orden<=0) throw new ValidacionException("El id de la orden debe ser mayor que cero.");
        try {
            if(con==null || con.getAutoCommit()) throw new ValidacionException("El consumo requiere la transacción de la orden.");
        } catch(SQLException e) { throw new DaoException("No se pudo comprobar la transacción de consumo.",e); }
        OrdenServicio servicio=ordenes.bloquearPorId(con,orden)
                .orElseThrow(()->new ValidacionException("No existe la orden indicada."));
        if(!"En Proceso".equals(servicio.getEstado())) throw new ValidacionException("Solo se consume al completar una orden En Proceso.");
        List<LineaRepuestoOrden> lineas=inventario.bloquearLineas(con,orden).stream()
                .sorted(Comparator.comparingInt(LineaRepuestoOrden::repuestoId)).toList();
        // Bloquear todos los repuestos en el mismo orden evita ciclos entre órdenes.
        for(LineaRepuestoOrden linea:lineas) precio(con,linea.repuestoId());
        Set<Integer> afectados=new HashSet<>();
        LocalDateTime ahora=LocalDateTime.now(reloj).withNano(0);
        LocalDate hoy=ahora.toLocalDate();
        for(LineaRepuestoOrden linea:lineas) {
            List<ExistenciaLote> lotes=inventario.bloquearLotesDisponibles(con,linea.repuestoId());
            List<ReservaOrden> reservas=inventario.bloquearReservas(con,orden,linea.repuestoId());
            sinConsumo(reservas);
            List<ReservaOrden> activas=reservas.stream().filter(r->r.estado().equals("ACTIVA")).toList();
            long reservado=activas.stream().mapToLong(ReservaOrden::cantidad).sum();
            if(reservado!=linea.cantidad()) throw new ValidacionException("Las reservas activas no cubren exactamente la cantidad requerida.");
            for(ReservaOrden reserva:activas) {
                ExistenciaLote lote=lotes.stream().filter(l->l.loteId()==reserva.loteId()).findFirst()
                        .orElseThrow(()->new ValidacionException("No existe el lote reservado."));
                if(lote.fechaCaducidad()!=null && lote.fechaCaducidad().isBefore(hoy)) {
                    throw new ValidacionException("Hay un lote reservado vencido; ajusta la reserva antes de completar.");
                }
                if(ahora.isBefore(reserva.fechaReserva())) throw new ValidacionException("El consumo no puede ser anterior a la reserva.");
                if(!inventario.descontarLote(con,lote.loteId(),linea.repuestoId(),reserva.cantidad())
                        || !inventario.consumirReserva(con,reserva.id(),ahora)) {
                    throw new ValidacionException("El stock o la reserva cambió; no se pudo confirmar el consumo.");
                }
            }
            afectados.add(linea.repuestoId());
        }
        // Sin commit, cierre ni publicación: OrdenServicioService coordina todo.
        return Set.copyOf(afectados);
    }

    private OrdenServicio abierta(Connection con,int orden) {
        OrdenServicio o=ordenes.bloquearPorId(con,orden)
                .orElseThrow(()->new ValidacionException("No existe la orden indicada."));
        if(!List.of("Pendiente","En Proceso").contains(o.getEstado())) {
            throw new ValidacionException("La orden está cerrada y no permite modificar reservas.");
        }
        return o;
    }
    private BigDecimal precio(Connection con,int repuesto) {
        return inventario.bloquearPrecio(con,repuesto)
                .orElseThrow(()->new ValidacionException("No existe el repuesto indicado."));
    }
    private void sinConsumo(List<ReservaOrden> reservas) {
        if(reservas.stream().anyMatch(r->r.estado().equals("CONSUMIDA"))) {
            throw new ValidacionException("La línea ya tiene consumo; no se puede descontar de nuevo ni reajustar.");
        }
    }
    private void validarIds(int orden,int repuesto) {
        if(orden<=0 || repuesto<=0) throw new ValidacionException("Los identificadores deben ser mayores que cero.");
    }
    private void transaccion(Consumer<Connection> accion) {
        try(Connection con=conexiones.get()) {
            con.setAutoCommit(false);
            try { accion.accept(con);con.commit(); }
            catch(RuntimeException|SQLException e) {
                try { con.rollback(); } catch(SQLException rollback) { e.addSuppressed(rollback); }
                if(e instanceof RuntimeException runtime) throw runtime;
                throw new DaoException("No se pudo confirmar la operación de reservas.",e);
            }
        } catch(SQLException e) { throw new DaoException("No se pudo administrar la conexión de reservas.",e); }
    }
}
