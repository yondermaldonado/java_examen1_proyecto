package com.carmotors.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** MODELO: parte de un Lote. LoteDAOJdbc guarda cada linea en lotes_repuestos.
 * Un repuesto que llega dentro de un lote: cuánto llegó, a qué costo y cuándo caduca (opcional). */
public class LoteLinea {
    private int repuestoId;
    private int cantidadIngresada;
    private BigDecimal costoUnitario;
    private LocalDate fechaCaducidad; // null = el repuesto no caduca

    public int getRepuestoId() { return repuestoId; }
    public void setRepuestoId(int repuestoId) { this.repuestoId = repuestoId; }
    public int getCantidadIngresada() { return cantidadIngresada; }
    public void setCantidadIngresada(int cantidadIngresada) { this.cantidadIngresada = cantidadIngresada; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public void setCostoUnitario(BigDecimal costoUnitario) { this.costoUnitario = costoUnitario; }
    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public void setFechaCaducidad(LocalDate fechaCaducidad) { this.fechaCaducidad = fechaCaducidad; }
}