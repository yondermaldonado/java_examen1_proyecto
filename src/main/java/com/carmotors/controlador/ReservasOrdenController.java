package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.servicio.ReservasOrdenService;
import java.util.Objects;
import java.util.function.Supplier;

/** Adaptación para la futura consola; no descuenta stock directamente. */
public class ReservasOrdenController {
    private final ReservasOrdenService servicio;
    public ReservasOrdenController(ReservasOrdenService servicio) { this.servicio=Objects.requireNonNull(servicio); }
    public String reservar(String orden,String repuesto,String cantidad) {
        return ejecutar(()->{servicio.reservar(entero(orden),entero(repuesto),entero(cantidad));return "Repuesto reservado.";});
    }
    public String ajustar(String orden,String repuesto,String cantidadTotal) {
        return ejecutar(()->{servicio.ajustar(entero(orden),entero(repuesto),entero(cantidadTotal));return "Reserva ajustada.";});
    }
    public String liberar(String orden,String repuesto) {
        return ejecutar(()->{servicio.liberar(entero(orden),entero(repuesto));return "Reserva liberada; la línea sigue pendiente de cubrir.";});
    }
    private int entero(String texto) { return Integer.parseInt(texto==null?"":texto.trim()); }
    private String ejecutar(Supplier<String> accion) {
        try{return accion.get();}
        catch(NumberFormatException e){return "Error: escribe identificadores y cantidades enteros.";}
        catch(ValidacionException e){return "Error: "+e.getMessage();}
        catch(DaoException|ConexionException e){return "Error del sistema: "+e.getMessage();}
    }
}
