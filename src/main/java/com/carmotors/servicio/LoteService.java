package com.carmotors.servicio;

import com.carmotors.dao.LoteDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.AsignacionLote;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.Lote;
import com.carmotors.modelo.LoteLinea;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * SERVICIO: recibe peticiones de LoteController y aplica reglas del negocio.
 * Usa LoteDAO para leer o guardar y PublicadorInventario para avisar despues de commit.
 * Aqui se controlan las transacciones, nunca dentro de LoteDAOJdbc.
 */
public class LoteService {
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99"); // DECIMAL(10,2)
    private static final int CODIGO_MAXIMO = 50;

    private final LoteDAO dao;
    private final ProveedorConsulta proveedores;
    private final Supplier<Connection> conexiones;
    private final Supplier<LocalDate> hoy;
    private final PublicadorInventario publicador;

    /**
     * En producción: new LoteService(new LoteDAOFactory().obtenerDAO(), proveedores,
     *     () -> ConexionFactory.getInstancia().crearConexion(), LocalDate::now)
     * En pruebas, "hoy" se fija: () -> LocalDate.of(2026, 11, 13)
     */
    public LoteService(LoteDAO dao, ProveedorConsulta proveedores,
                       Supplier<Connection> conexiones, Supplier<LocalDate> hoy) {
        this(dao, proveedores, conexiones, hoy, new PublicadorInventario());
    }

    public LoteService(LoteDAO dao, ProveedorConsulta proveedores,
                       Supplier<Connection> conexiones, Supplier<LocalDate> hoy,
                       PublicadorInventario publicador) {
        this.dao = dao;
        this.proveedores = proveedores;
        this.conexiones = conexiones;
        this.hoy = hoy;
        this.publicador = publicador;
    }

    // ---------- Ingreso: valida primero y guarda lote y líneas en UNA transacción ----------

    /**
     * PASO 1: valida el lote.
     * PASO 2: llama a LoteDAO.crear dentro de una transaccion.
     * PASO 3: cuando commit termina correctamente, avisa al Observer.
     * PASO 4: devuelve al Controller el ID creado.
     */
    public int registrarIngreso(Lote lote) {
        validar(lote);
        int id = enTransaccion(con -> dao.crear(con, lote));
        // Solo después del commit confirmado. Si hubo rollback, no llega aquí.
        for (LoteLinea linea : lote.getLineas()) {
            publicador.publicar(linea.getRepuestoId());
        }
        return id;
    }

    // ---------- Trazabilidad ----------

    /** Trazabilidad por codigo: consulta al DAO y devuelve las lineas del lote. */
    public List<ExistenciaLote> buscarPorCodigo(String codigo) {
        String buscado = codigo == null ? "" : codigo.trim();
        return leer(con -> dao.buscarPorCodigo(con, buscado));
    }

    /** Trazabilidad por repuesto: pide al DAO todos sus lotes. */
    public List<ExistenciaLote> listarPorRepuesto(int repuestoId) {
        return leer(con -> dao.listarPorRepuesto(con, repuestoId));
    }

    /** Trazabilidad por proveedor: pide al DAO sus ingresos registrados. */
    public List<ExistenciaLote> listarPorProveedor(int proveedorId) {
        return leer(con -> dao.listarPorProveedor(con, proveedorId));
    }

    // ---------- Stock: siempre calculado desde los lotes, nunca guardado aparte ----------

    /** Solicita al DAO las unidades fisicas existentes del repuesto. */
    public int stockFisico(int repuestoId) {
        return leer(con -> dao.stockFisico(con, repuestoId));
    }

    /** Solicita al DAO fisico y reservas activas; resta ambos valores. */
    public int stockDisponible(int repuestoId) {
        return leer(con -> dao.stockFisico(con, repuestoId) - dao.stockReservado(con, repuestoId));
    }

    // ---------- FEFO ----------

    /** Propone de qué lotes tomar la cantidad pedida. No reserva nada: solo devuelve el plan. */
    public List<AsignacionLote> seleccionarFefo(int repuestoId, int cantidad) {
        if (repuestoId <= 0) {
            throw new ValidacionException("El repuesto es obligatorio.");
        }
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad debe ser mayor que 0.");
        }
        List<ExistenciaLote> candidatos = leer(con -> dao.listarPorRepuesto(con, repuestoId));
        return PoliticaFefo.asignar(candidatos, cantidad, hoy.get());
    }

    // ---------- Validaciones ----------

    /** Verifica los datos generales y cada linea antes de entrar a MySQL. */
    private void validar(Lote lote) {
        if (lote == null) {
            throw new ValidacionException("El lote no puede ser nulo.");
        }
        if (lote.getCodigo() == null || lote.getCodigo().isBlank()) {
            throw new ValidacionException("El código del lote es obligatorio.");
        }
        if (lote.getCodigo().trim().length() > CODIGO_MAXIMO) {
            throw new ValidacionException("El código del lote no puede pasar de " + CODIGO_MAXIMO + " caracteres.");
        }
        if (lote.getProveedorId() <= 0) {
            throw new ValidacionException("El proveedor es obligatorio.");
        }
        if (!proveedores.existe(lote.getProveedorId())) {
            throw new ValidacionException("El proveedor " + lote.getProveedorId() + " no existe.");
        }
        if (lote.getFechaIngreso() == null) {
            throw new ValidacionException("La fecha de ingreso es obligatoria.");
        }
        if (lote.getFechaIngreso().isAfter(hoy.get())) {
            throw new ValidacionException("La fecha de ingreso no puede ser futura.");
        }
        if (lote.getLineas() == null || lote.getLineas().isEmpty()) {
            throw new ValidacionException("El lote debe tener al menos un repuesto.");
        }
        Set<Integer> repuestosVistos = new HashSet<>();
        for (LoteLinea linea : lote.getLineas()) {
            validarLinea(linea, lote.getFechaIngreso());
            if (!repuestosVistos.add(linea.getRepuestoId())) {
                throw new ValidacionException("El repuesto " + linea.getRepuestoId() + " está repetido en el lote.");
            }
        }
        // Dejar los datos listos para guardar
        lote.setCodigo(lote.getCodigo().trim());
        for (LoteLinea linea : lote.getLineas()) {
            linea.setCostoUnitario(linea.getCostoUnitario().setScale(2));
        }
    }

    /** Revisa el repuesto, las cantidades, el costo y su caducidad. */
    private void validarLinea(LoteLinea linea, LocalDate fechaIngreso) {
        if (linea == null) {
            throw new ValidacionException("Hay una línea vacía en el lote.");
        }
        if (linea.getRepuestoId() <= 0) {
            throw new ValidacionException("El repuesto es obligatorio.");
        }
        if (linea.getCantidadIngresada() <= 0) {
            throw new ValidacionException("La cantidad ingresada debe ser mayor que 0.");
        }
        validarCosto(linea.getCostoUnitario());
        LocalDate caducidad = linea.getFechaCaducidad();
        if (caducidad != null && caducidad.isBefore(fechaIngreso)) {
            throw new ValidacionException("La fecha de caducidad no puede ser anterior a la fecha de ingreso.");
        }
    }

    /** Comprueba que el costo se pueda guardar en DECIMAL(10,2). */
    private void validarCosto(BigDecimal costo) {
        if (costo == null) {
            throw new ValidacionException("El costo unitario es obligatorio.");
        }
        if (costo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El costo unitario no puede ser negativo.");
        }
        if (costo.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException("El costo unitario puede tener máximo 2 decimales.");
        }
        if (costo.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ValidacionException("El costo unitario no puede pasar de " + PRECIO_MAXIMO.toPlainString() + ".");
        }
    }

    // ---------- Manejo de la conexión ----------

    /** Abre la conexión, ejecuta la acción y la cierra. Para consultas. */
    /**
     * Para consultas: abre una conexion, ejecuta la lambda que llama al DAO,
     * devuelve el resultado y cierra la conexion con try-with-resources.
     */
    private <T> T leer(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            return accion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión.", e);
        }
    }

    /** Igual, pero con commit si todo sale bien y rollback si algo falla. Para cambios. */
    /**
     * Para escrituras: abre conexion, desactiva auto-commit, ejecuta el DAO
     * y confirma (commit). Ante un error, deshace (rollback).
     * No publica aqui: registrarIngreso publica solo si este metodo regresa bien.
     */
    private <T> T enTransaccion(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            con.setAutoCommit(false);
            try {
                T resultado = accion.apply(con);
                con.commit();
                return resultado;
            } catch (RuntimeException e) {
                con.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new DaoException("Error al manejar la transacción.", e);
        }
    }
}