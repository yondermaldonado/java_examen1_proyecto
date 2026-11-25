package com.carmotors.modelo;

import java.time.LocalDate;

/**
 * Representa las unidades de UN repuesto dentro de UN lote.
 *
 * Recibe datos desde ExistenciasDAOJdbc y se utiliza en ExistenciasService.
 * Esta clase NO guarda información en MySQL ni modifica el inventario.
 */
public class StockLote {

    private final int loteId;
    private final String codigoLote;
    private final int repuestoId;
    private final LocalDate fechaCaducidad;
    private final long fisico;
    private final long reservadoActivo;

    // El DAO utiliza este constructor al leer una fila de MySQL.
    // Las pruebas también lo utilizan para crear lotes de ejemplo.
    public StockLote(int loteId, String codigoLote, int repuestoId,
                     LocalDate fechaCaducidad, long fisico, long reservadoActivo) {
        // PASO 1. Comprobar que las cantidades sean consistentes.
        if (loteId <= 0 || repuestoId <= 0) {
            throw new IllegalArgumentException("Los ID deben ser mayores que cero.");
        }
        if (fisico < 0 || reservadoActivo < 0) {
            throw new IllegalArgumentException("Las cantidades no pueden ser negativas.");
        }
        if (reservadoActivo > fisico) {
            throw new IllegalArgumentException("La reserva no puede superar el stock fisico.");
        }

        // PASO 2. Guardar los datos. No hay setters: es una consulta de solo lectura.
        this.loteId = loteId;
        this.codigoLote = codigoLote;
        this.repuestoId = repuestoId;
        this.fechaCaducidad = fechaCaducidad;
        this.fisico = fisico;
        this.reservadoActivo = reservadoActivo;
    }

    public int getLoteId() { return loteId; }
    public String getCodigoLote() { return codigoLote; }
    public int getRepuestoId() { return repuestoId; }
    public LocalDate getFechaCaducidad() { return fechaCaducidad; }
    public long getFisico() { return fisico; }
    public long getReservadoActivo() { return reservadoActivo; }

    // Stock disponible = unidades fisicas - reservas activas.
    // Reservar NO significa consumir ni descontar unidades fisicas.
    public long getDisponible() {
        return fisico - reservadoActivo;
    }

    // Un lote sin caducidad sigue vigente. Si vence hoy, aún es vigente.
    public boolean vigente(LocalDate fechaActual) {
        if (fechaCaducidad == null) {
            return true;
        }
        return !fechaCaducidad.isBefore(fechaActual);
    }

    // El disponible utilizable excluye los lotes ya vencidos.
    public long disponibleUtilizable(LocalDate fechaActual) {
        if (!vigente(fechaActual)) {
            return 0;
        }
        return getDisponible();
    }

    // COMPATIBILIDAD con el código existente en develop.
    // Las pruebas, Observer y el menú todavía usan la sintaxis de los record.
    // Se conservan estos métodos para no tener que cambiar otros módulos.
    public int loteId() { return getLoteId(); }
    public String codigoLote() { return getCodigoLote(); }
    public int repuestoId() { return getRepuestoId(); }
    public LocalDate fechaCaducidad() { return getFechaCaducidad(); }
    public long fisico() { return getFisico(); }
    public long reservadoActivo() { return getReservadoActivo(); }
    public long disponible() { return getDisponible(); }

    // Permite imprimir un lote en el menú sin mostrar una referencia de memoria.
    @Override
    public String toString() {
        return "Lote " + codigoLote + " | Repuesto " + repuestoId
                + " | Fisico: " + fisico + " | Reservado: " + reservadoActivo
                + " | Disponible: " + getDisponible()
                + " | Caducidad: " + (fechaCaducidad == null ? "Sin caducidad" : fechaCaducidad);
    }
}
