package com.carmotors.controlador;

import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.servicio.OrdenServicioService;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Adaptador de la futura consola, sin SQL ni entrada/salida directa. */
public class OrdenServicioController {
    private final OrdenServicioService servicio;

    public OrdenServicioController(OrdenServicioService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public String crear(String vehiculoId, String tipo, String problema) {
        return ejecutar(() -> "Orden creada: " + formatear(servicio.crearOrden(entero(vehiculoId), tipo, problema)));
    }

    public String buscarPorId(String id) {
        return ejecutar(() -> servicio.buscarPorId(entero(id)).map(this::formatear).orElse("No existe una orden con ese id."));
    }

    public String listar() {
        return ejecutar(() -> filas(servicio.listar()));
    }

    public String listarPorVehiculo(String id) {
        return ejecutar(() -> filas(servicio.listarPorVehiculo(entero(id))));
    }

    public String listarPorEstado(String estado) {
        return ejecutar(() -> filas(servicio.listarPorEstado(estado)));
    }

    /** Propaga errores para que la vista no intente exportar una factura que no se confirmó. */
    public void facturar(String id) { servicio.facturarOrden(entero(id)); }

    public String cambiarEstado(String id, String estado) {
        return ejecutar(() -> { servicio.cambiarEstado(entero(id), estado); return "Estado de la orden actualizado."; });
    }

    private int entero(String id) {
        try { return Integer.parseInt(id == null ? "" : id.trim()); }
        catch (NumberFormatException e) { throw new ValidacionException("Escribe un identificador entero válido."); }
    }

    private String filas(List<OrdenServicio> ordenes) {
        return ordenes.isEmpty() ? "No hay órdenes para esta consulta."
                : ordenes.stream().map(this::formatear).collect(Collectors.joining(System.lineSeparator()));
    }

    private String formatear(OrdenServicio o) {
        return "#" + o.getId() + " | vehículo: " + o.getPlaca() + " (#" + o.getVehiculoId() + ")"
                + " | cliente: #" + o.getClienteId() + " | " + o.getTipoMantenimiento()
                + " | " + o.getEstado() + " | recepción: " + o.getFechaRecepcion()
                + " | problema: " + o.getDescripcionProblema()
                + " | mano de obra: " + o.getCostoManoObra().toPlainString();
    }

    private String ejecutar(Supplier<String> accion) {
        try { return accion.get(); }
        catch (ValidacionException e) { return "Error: " + e.getMessage(); }
        catch (DaoException | ConexionException | AccesoDatosException e) { return "Error del sistema: " + e.getMessage(); }
    }
}
