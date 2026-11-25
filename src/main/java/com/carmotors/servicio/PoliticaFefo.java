package com.carmotors.servicio;

import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Política FEFO (First Expired, First Out): sale primero lo que caduca primero.
 *
 * Reglas:
 *  1. Solo se usan lotes vigentes: sin caducidad, o con caducidad igual o posterior a hoy.
 *  2. Solo se usan lotes con unidades disponibles (cantidadDisponible mayor que 0).
 *  3. Orden: primero los que sí caducan, del que vence antes al que vence después.
 *  4. Los lotes SIN caducidad van al final.
 *  5. Si dos lotes empatan (misma caducidad, o ambos sin caducidad), sale primero el que
 *     ingresó antes (FIFO); si aún empatan, el de menor id.
 *  6. Si un lote no alcanza, se completa con el siguiente (la cantidad se reparte).
 *
 * LoteService.seleccionarFefo llama a asignar().
 * Es una clase sin base de datos: recibe los candidatos y la fecha de hoy, y devuelve el plan.
 */
public final class PoliticaFefo {

    private PoliticaFefo() {
    }

    /** Solo prepara un plan: NO actualiza lotes, NO reserva y NO consume. */
    public static List<AsignacionLote> asignar(List<ExistenciaLote> candidatos, int cantidad, LocalDate hoy) {
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que 0.");
        }
        List<ExistenciaLote> utilizables = new ArrayList<>();
        long totalUtilizable = 0;
        for (ExistenciaLote e : candidatos) {
            boolean vigente = e.getFechaCaducidad() == null || !e.getFechaCaducidad().isBefore(hoy);
            if (vigente && e.getCantidadDisponible() > 0) {
                utilizables.add(e);
                totalUtilizable = Math.addExact(totalUtilizable, e.getCantidadDisponible());
            }
        }
        if (totalUtilizable < cantidad) {
            throw new ValidacionException("Stock insuficiente: se piden " + cantidad
                    + " unidades y solo hay " + totalUtilizable + " disponibles en lotes vigentes.");
        }
        utilizables.sort(PoliticaFefo::comparar);

        List<AsignacionLote> plan = new ArrayList<>();
        int faltante = cantidad;
        for (ExistenciaLote e : utilizables) {
            if (faltante == 0) {
                break;
            }
            int tomar = Math.min(faltante, e.getCantidadDisponible());
            plan.add(new AsignacionLote(e.getLoteId(), e.getCodigo(), e.getRepuestoId(), tomar, e.getFechaCaducidad()));
            faltante -= tomar;
        }
        return plan;
    }

    /** Ordena primero por caducidad, luego por fecha de ingreso y finalmente por ID. */
    private static int comparar(ExistenciaLote a, ExistenciaLote b) {
        boolean aSinCaducidad = a.getFechaCaducidad() == null;
        boolean bSinCaducidad = b.getFechaCaducidad() == null;
        if (aSinCaducidad != bSinCaducidad) {
            return aSinCaducidad ? 1 : -1;              // los que sí caducan van primero
        }
        if (!aSinCaducidad) {
            int porCaducidad = a.getFechaCaducidad().compareTo(b.getFechaCaducidad());
            if (porCaducidad != 0) {
                return porCaducidad;                     // vence antes, sale antes
            }
        }
        int porIngreso = a.getFechaIngreso().compareTo(b.getFechaIngreso());
        if (porIngreso != 0) {
            return porIngreso;                           // empate: el más antiguo primero
        }
        return Integer.compare(a.getLoteId(), b.getLoteId());
    }
}