package com.carmotors.modelo;

import java.time.LocalDate;

/** Resultado del calculo FEFO. No guarda ni consume existencias. */
public class AsignacionLote {

    // Dato recibido de la consulta o del calculo del servicio.
    private final int loteId;
    // Dato recibido de la consulta o del calculo del servicio.
    private final String codigo;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int repuestoId;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int cantidad;
    // Dato recibido de la consulta o del calculo del servicio.
    private final LocalDate fechaCaducidad;

    // Constructor: recibe los valores de todos los atributos.
    public AsignacionLote(
            int loteId,
            String codigo,
            int repuestoId,
            int cantidad,
            LocalDate fechaCaducidad
    ) {
        this.loteId = loteId;
        this.codigo = codigo;
        this.repuestoId = repuestoId;
        this.cantidad = cantidad;
        this.fechaCaducidad = fechaCaducidad;
    }

    // Devuelve loteId. No consulta la base de datos.
    public int getLoteId() {
        return loteId;
    }

    // Devuelve codigo. No consulta la base de datos.
    public String getCodigo() {
        return codigo;
    }

    // Devuelve repuestoId. No consulta la base de datos.
    public int getRepuestoId() {
        return repuestoId;
    }

    // Devuelve cantidad. No consulta la base de datos.
    public int getCantidad() {
        return cantidad;
    }

    // Devuelve fechaCaducidad. No consulta la base de datos.
    public LocalDate getFechaCaducidad() {
        return fechaCaducidad;
    }

    // Metodos de compatibilidad: otras clases de develop aun usan nombres de record.
    // Se mantienen para evitar errores mientras se actualizan esos modulos.
    public int loteId() {
        return getLoteId();
    }

    public String codigo() {
        return getCodigo();
    }

    public int repuestoId() {
        return getRepuestoId();
    }

    public int cantidad() {
        return getCantidad();
    }

    public LocalDate fechaCaducidad() {
        return getFechaCaducidad();
    }

}
