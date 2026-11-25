package com.carmotors.modelo;

/**
 * Resumen de existencias de UN repuesto, sumadas desde sus lotes.
 * ExistenciasService construye este objeto y el menú muestra los resultados.
 * No guarda datos, no reserva unidades y no hace consultas SQL.
 */
public class StockRepuesto {
    private final int repuestoId;
    private final long fisico;
    private final long reservadoActivo;
    private final long disponible;
    private final long disponibleUtilizable;

    // Constructor tradicional en lugar de record.
    public StockRepuesto(int repuestoId, long fisico, long reservadoActivo,
                         long disponible, long disponibleUtilizable) {
        this.repuestoId = repuestoId;
        this.fisico = fisico;
        this.reservadoActivo = reservadoActivo;
        this.disponible = disponible;
        this.disponibleUtilizable = disponibleUtilizable;
    }

    public int getRepuestoId() { return repuestoId; }
    public long getFisico() { return fisico; }
    public long getReservadoActivo() { return reservadoActivo; }
    public long getDisponible() { return disponible; }
    public long getDisponibleUtilizable() { return disponibleUtilizable; }

    // Métodos antiguos: se mantienen para compatibilidad con otros módulos.
    public int repuestoId() { return getRepuestoId(); }
    public long fisico() { return getFisico(); }
    public long reservadoActivo() { return getReservadoActivo(); }
    public long disponible() { return getDisponible(); }
    public long disponibleUtilizable() { return getDisponibleUtilizable(); }

    @Override
    public String toString() {
        return "Repuesto " + repuestoId + " | Fisico: " + fisico
                + " | Reservado: " + reservadoActivo
                + " | Disponible: " + disponible
                + " | Utilizable: " + disponibleUtilizable;
    }
}
