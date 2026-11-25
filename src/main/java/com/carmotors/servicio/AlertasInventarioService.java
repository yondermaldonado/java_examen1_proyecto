package com.carmotors.servicio;

import com.carmotors.dao.AlertasDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AlertaInventario;
import com.carmotors.modelo.RepuestoMinimo;
import com.carmotors.modelo.StockLote;
import com.carmotors.modelo.StockRepuesto;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * SERVICE + OBSERVER:
 * 1. AlertasDAO entrega el nombre y el minimo de cada repuesto.
 * 2. ExistenciasService entrega las cantidades y fechas de los lotes.
 * 3. Este servicio decide si corresponde crear una alerta.
 * 4. PublicadorInventario llama a inventarioCambio() tras un commit.
 * No modifica MySQL ni envía mensajes externos.
 */
public class AlertasInventarioService implements ObservadorInventario {
    private final AlertasDAO dao;
    private final ExistenciasService existencias;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;
    private final int diasAviso;
    private List<AlertaInventario> ultimasAlertas = new ArrayList<>();

    // Recibe las dependencias, la fecha de referencia y los dias de aviso.
    public AlertasInventarioService(AlertasDAO dao,
                                   ExistenciasService existencias,
                                   Supplier<Connection> conexiones,
                                   Supplier<LocalDate> hoy,
                                   int diasAviso) {
        if (dao == null || existencias == null || conexiones == null || hoy == null) {
            throw new IllegalArgumentException("Las dependencias son obligatorias.");
        }
        if (diasAviso < 0) {
            throw new ValidacionException("Los dias de aviso no pueden ser negativos.");
        }

        this.dao = dao;
        this.existencias = existencias;
        this.conexiones = conexiones;
        this.hoy = hoy;
        this.diasAviso = diasAviso;
    }

    /** Consulta todos los repuestos y calcula las alertas actuales. */
    public List<AlertaInventario> consultarAlertas() {
        // Primero pedimos al DAO la lista de repuestos con sus minimos.
        List<RepuestoMinimo> repuestos = obtenerRepuestos();
        List<AlertaInventario> alertas = new ArrayList<>();
        LocalDate fechaActual = hoy.get();

        // Despues analizamos cada repuesto por separado.
        for (RepuestoMinimo repuesto : repuestos) {
            revisarStockBajo(repuesto, alertas);
            revisarCaducidad(repuesto, fechaActual, alertas);
        }

        // Guardamos el resultado para consultarlo sin volver a calcular.
        ultimasAlertas = new ArrayList<>(alertas);
        return alertas;
    }

    /** La consulta SQL se hace unicamente en el DAO. */
    private List<RepuestoMinimo> obtenerRepuestos() {
        try (Connection conexion = conexiones.get()) {
            return dao.listarRepuestos(conexion);
        } catch (SQLException error) {
            throw new DaoException("Error al cerrar la conexion de alertas.", error);
        }
    }

    /** Crea una alerta cuando el disponible utilizable es MENOR que el minimo. */
    private void revisarStockBajo(RepuestoMinimo repuesto, List<AlertaInventario> alertas) {
        // El modulo de existencias realiza los calculos de inventario.
        StockRepuesto stock = existencias.resumen(repuesto.getId());
        long disponible = stock.disponibleUtilizable();
        int minimo = repuesto.getStockMinimo();

        if (disponible < minimo) {
            AlertaInventario alerta = new AlertaInventario(
                    "STOCK_BAJO", repuesto.getId(), repuesto.getNombre(),
                    null, null, null, disponible, minimo);
            alertas.add(alerta);
        }
    }

    /** Revisa los lotes con fecha de caducidad dentro del plazo configurado. */
    private void revisarCaducidad(RepuestoMinimo repuesto,
                                  LocalDate fechaActual,
                                  List<AlertaInventario> alertas) {
        // Ejemplo: si el aviso es de 7 dias, revisamos desde hoy hasta hoy + 7.
        LocalDate fechaLimite = fechaActual.plusDays(diasAviso);
        List<StockLote> lotes = existencias.porRepuesto(repuesto.getId());

        for (StockLote lote : lotes) {
            LocalDate caducidad = lote.fechaCaducidad();

            // Si no tiene fecha de caducidad, no hay alerta de vencimiento.
            if (caducidad == null) {
                continue;
            }

            // Si ya vencio, no es una caducidad PROXIMA.
            if (caducidad.isBefore(fechaActual)) {
                continue;
            }

            // Si vence despues del plazo de aviso, todavia no debemos alertar.
            if (caducidad.isAfter(fechaLimite)) {
                continue;
            }

            // Si el lote esta agotado, no hay unidades para alertar.
            if (lote.fisico() <= 0) {
                continue;
            }

            AlertaInventario alerta = new AlertaInventario(
                    "CADUCIDAD_PROXIMA", repuesto.getId(), repuesto.getNombre(),
                    lote.loteId(), lote.codigoLote(), caducidad, lote.fisico(), 0);
            alertas.add(alerta);
        }
    }

    /** Observer: al recibir el evento, actualiza las alertas guardadas en memoria. */
    @Override
    public void inventarioCambio(int repuestoId) {
        // Un evento confirmado obliga a recalcular el reporte en memoria.
        consultarAlertas();
    }

    /** Devuelve la ultima lista calculada sin permitir modificar la lista interna. */
    public List<AlertaInventario> ultimasAlertas() {
        return new ArrayList<>(ultimasAlertas);
    }

    /** Genera un texto sencillo para mostrar desde una consola. */
    public String reporteConsola() {
        List<AlertaInventario> alertas = consultarAlertas();

        if (alertas.isEmpty()) {
            return "Sin alertas de inventario.";
        }

        StringBuilder reporte = new StringBuilder();
        reporte.append("ALERTAS DE INVENTARIO\n");

        for (AlertaInventario alerta : alertas) {
            reporte.append("- ");
            reporte.append(alerta.toString());
            reporte.append("\n");
        }

        return reporte.toString();
    }
}
