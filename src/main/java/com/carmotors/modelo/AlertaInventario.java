package com.carmotors.modelo;

import java.time.LocalDate;

/**
 * MODELO: representa una alerta que ya fue calculada.
 * La crea AlertasInventarioService y la muestra el reporte de consola.
 * No guarda ni modifica cantidades en MySQL.
 */
public class AlertaInventario {
    private final String tipo;
    private final int repuestoId;
    private final String nombreRepuesto;
    private final Integer loteId;
    private final String codigoLote;
    private final LocalDate caducidad;
    private final long cantidad;
    private final long minimo;

    // Constructor normal: recibe todos los datos de la alerta.
    public AlertaInventario(String tipo, int repuestoId, String nombreRepuesto,
                           Integer loteId, String codigoLote, LocalDate caducidad,
                           long cantidad, long minimo) {
        this.tipo = tipo;
        this.repuestoId = repuestoId;
        this.nombreRepuesto = nombreRepuesto;
        this.loteId = loteId;
        this.codigoLote = codigoLote;
        this.caducidad = caducidad;
        this.cantidad = cantidad;
        this.minimo = minimo;
    }

    // Getters: permiten consultar los datos de la alerta desde Service y Test.
    public String getTipo() { return tipo; }
    public int getRepuestoId() { return repuestoId; }
    public String getNombreRepuesto() { return nombreRepuesto; }
    public Integer getLoteId() { return loteId; }
    public String getCodigoLote() { return codigoLote; }
    public LocalDate getCaducidad() { return caducidad; }
    public long getCantidad() { return cantidad; }
    public long getMinimo() { return minimo; }

    // Convierte la alerta en una linea legible para la consola.
    @Override
    public String toString() {
        if ("STOCK_BAJO".equals(tipo)) {
            return "STOCK BAJO | " + nombreRepuesto + " (#" + repuestoId
                    + ") | disponible: " + cantidad + " | mínimo: " + minimo;
        }
        return "CADUCIDAD PROXIMA | " + nombreRepuesto + " | lote: " + codigoLote
                + " | vence: " + caducidad + " | unidades: " + cantidad;
    }
}
