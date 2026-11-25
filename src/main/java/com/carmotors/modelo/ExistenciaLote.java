package com.carmotors.modelo;

import java.time.LocalDate;
import java.math.BigDecimal;

/** Datos de trazabilidad de un repuesto dentro de un lote. Los crea LoteDAOJdbc y los leen LoteService y PoliticaFefo. */
public class ExistenciaLote {

    // Dato recibido de la consulta o del calculo del servicio.
    private final int loteId;
    // Dato recibido de la consulta o del calculo del servicio.
    private final String codigo;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int proveedorId;
    // Dato recibido de la consulta o del calculo del servicio.
    private final String proveedor;
    // Dato recibido de la consulta o del calculo del servicio.
    private final LocalDate fechaIngreso;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int repuestoId;
    // Dato recibido de la consulta o del calculo del servicio.
    private final String repuesto;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int cantidadIngresada;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int cantidadActual;
    // Dato recibido de la consulta o del calculo del servicio.
    private final int cantidadDisponible;
    // Dato recibido de la consulta o del calculo del servicio.
    private final BigDecimal costoUnitario;
    // Dato recibido de la consulta o del calculo del servicio.
    private final LocalDate fechaCaducidad;

    // Constructor: recibe los valores de todos los atributos.
    public ExistenciaLote(
            int loteId,
            String codigo,
            int proveedorId,
            String proveedor,
            LocalDate fechaIngreso,
            int repuestoId,
            String repuesto,
            int cantidadIngresada,
            int cantidadActual,
            int cantidadDisponible,
            BigDecimal costoUnitario,
            LocalDate fechaCaducidad
    ) {
        this.loteId = loteId;
        this.codigo = codigo;
        this.proveedorId = proveedorId;
        this.proveedor = proveedor;
        this.fechaIngreso = fechaIngreso;
        this.repuestoId = repuestoId;
        this.repuesto = repuesto;
        this.cantidadIngresada = cantidadIngresada;
        this.cantidadActual = cantidadActual;
        this.cantidadDisponible = cantidadDisponible;
        this.costoUnitario = costoUnitario;
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

    // Devuelve proveedorId. No consulta la base de datos.
    public int getProveedorId() {
        return proveedorId;
    }

    // Devuelve proveedor. No consulta la base de datos.
    public String getProveedor() {
        return proveedor;
    }

    // Devuelve fechaIngreso. No consulta la base de datos.
    public LocalDate getFechaIngreso() {
        return fechaIngreso;
    }

    // Devuelve repuestoId. No consulta la base de datos.
    public int getRepuestoId() {
        return repuestoId;
    }

    // Devuelve repuesto. No consulta la base de datos.
    public String getRepuesto() {
        return repuesto;
    }

    // Devuelve cantidadIngresada. No consulta la base de datos.
    public int getCantidadIngresada() {
        return cantidadIngresada;
    }

    // Devuelve cantidadActual. No consulta la base de datos.
    public int getCantidadActual() {
        return cantidadActual;
    }

    // Devuelve cantidadDisponible. No consulta la base de datos.
    public int getCantidadDisponible() {
        return cantidadDisponible;
    }

    // Devuelve costoUnitario. No consulta la base de datos.
    public BigDecimal getCostoUnitario() {
        return costoUnitario;
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

    public int proveedorId() {
        return getProveedorId();
    }

    public String proveedor() {
        return getProveedor();
    }

    public LocalDate fechaIngreso() {
        return getFechaIngreso();
    }

    public int repuestoId() {
        return getRepuestoId();
    }

    public String repuesto() {
        return getRepuesto();
    }

    public int cantidadIngresada() {
        return getCantidadIngresada();
    }

    public int cantidadActual() {
        return getCantidadActual();
    }

    public int cantidadDisponible() {
        return getCantidadDisponible();
    }

    public BigDecimal costoUnitario() {
        return getCostoUnitario();
    }

    public LocalDate fechaCaducidad() {
        return getFechaCaducidad();
    }

}
