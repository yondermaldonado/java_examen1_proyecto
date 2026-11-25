package com.carmotors.servicio;

import com.carmotors.dao.ExistenciasDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.StockLote;
import com.carmotors.modelo.StockRepuesto;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Reglas de negocio para consultar existencias.
 * Recibe llamadas del menú y de otros servicios, como Observer.
 * No escribe SQL, no reserva y no consume unidades.
 *
 * SOLID: depende de la interfaz ExistenciasDAO y no de ExistenciasDAOJdbc.
 */
public class ExistenciasService {

    private final ExistenciasDAO dao;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;

    // ContextoInventario crea esta clase y le entrega el DAO y las conexiones.
    public ExistenciasService(ExistenciasDAO dao,
                             Supplier<Connection> conexiones,
                             Supplier<LocalDate> hoy) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
        this.hoy = Objects.requireNonNull(hoy);
    }

    // Devuelve los lotes de un repuesto.
    // La lambda entrega una conexión al DAO; el Service no escribe SQL.
    public List<StockLote> porRepuesto(int repuestoId) {
        validarId(repuestoId, "repuesto");
        return consultar(conexion -> dao.porRepuesto(conexion, repuestoId));
    }

    // Busca un par (lote, repuesto) y devuelve Optional.empty() si no está.
    public Optional<StockLote> porLote(int loteId, int repuestoId) {
        validarId(loteId, "lote");
        validarId(repuestoId, "repuesto");

        List<StockLote> lotes = consultar(
                conexion -> dao.porLote(conexion, loteId, repuestoId));

        if (lotes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lotes.get(0));
    }

    // Calcula el resumen de un repuesto sumando sus lotes.
    public StockRepuesto resumen(int repuestoId) {
        // PASO 1. Validar el identificador.
        validarId(repuestoId, "repuesto");

        // PASO 2. Tomar una misma fecha de referencia para todos los lotes.
        LocalDate fechaActual = hoy.get();

        // PASO 3. Pedir la información al DAO mediante porRepuesto().
        List<StockLote> lotes = porRepuesto(repuestoId);

        long totalFisico = 0;
        long totalReservado = 0;
        long totalUtilizable = 0;

        // PASO 4. Sumar cada cantidad por separado.
        for (StockLote lote : lotes) {
            totalFisico = Math.addExact(totalFisico, lote.getFisico());
            totalReservado = Math.addExact(totalReservado, lote.getReservadoActivo());
            long utilizableDelLote = lote.disponibleUtilizable(fechaActual);
            totalUtilizable = Math.addExact(totalUtilizable, utilizableDelLote);
        }

        // PASO 5. Las reservas reducen el disponible, NO el stock físico.
        long totalDisponible = totalFisico - totalReservado;

        // PASO 6. Devolver los totales al menú o al servicio que los pidió.
        return new StockRepuesto(repuestoId, totalFisico, totalReservado,
                totalDisponible, totalUtilizable);
    }

    // Evita enviar IDs incorrectos al DAO.
    private void validarId(int id, String nombre) {
        if (id <= 0) {
            throw new ValidacionException("El id de " + nombre + " debe ser mayor que cero.");
        }
    }

    // Abre una conexión de lectura, llama al DAO y cierra la conexión.
    // Se conserva la lambda y el método genérico que ya utiliza el proyecto.
    private <T> T consultar(Function<Connection, T> operacion) {
        try (Connection conexion = conexiones.get()) {
            return operacion.apply(conexion);
        } catch (SQLException error) {
            throw new DaoException("No se pudo cerrar la conexión al consultar existencias.", error);
        }
    }
}
