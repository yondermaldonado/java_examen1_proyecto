package com.carmotors.servicio;

import com.carmotors.dao.RepuestoDAO;
import com.carmotors.dao.factory.DAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Repuesto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

// SERVICE: aplica validaciones y coordina operaciones con el DAO.
// Recibe instrucciones de RepuestoController y publica cambios con Observer después del commit.
// Conservamos las lambdas para pasar acciones a leer() y enTransaccion().
public class RepuestoService {
    private static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99"); // DECIMAL(10,2)
    private static final int NOMBRE_MAXIMO = 255;

    private final RepuestoDAO dao;
    private final ProveedorConsulta proveedores;
    private final Supplier<Connection> conexiones;
    private final PublicadorInventario publicador;

    /** En producción: new RepuestoService(new RepuestoDAOFactory().obtenerDAO(), proveedores, () -> ConexionFactory.getInstancia().crearConexion()) */
    public RepuestoService(RepuestoDAO dao, ProveedorConsulta proveedores, Supplier<Connection> conexiones) {
        this(dao, proveedores, conexiones, new PublicadorInventario());
    }

    public RepuestoService(RepuestoDAO dao, ProveedorConsulta proveedores,
                           Supplier<Connection> conexiones, PublicadorInventario publicador) {
        this.dao = dao;
        this.proveedores = proveedores;
        this.conexiones = conexiones;
        this.publicador = publicador;
    }

    /** Permite seleccionar la implementación de persistencia mediante Factory Method. */
    public RepuestoService(DAOFactory<RepuestoDAO> creador, ProveedorConsulta proveedores,
                           Supplier<Connection> conexiones, PublicadorInventario publicador) {
        this(creador.obtenerDAO(), proveedores, conexiones, publicador);
    }

    // ---------- Cambios: validan primero y usan transacción ----------

    // PASO 1: validar. PASO 2: guardar mediante DAO dentro de una transacción.
    // PASO 3: cuando commit ya terminó, avisar a los observadores.
    public int crear(Repuesto repuesto) {
        validar(repuesto);
        int id = enTransaccion(con -> dao.crear(con, repuesto));
        publicador.publicar(id);
        return id;
    }

    // Actualizar sigue el mismo recorrido que crear y solo notifica si hubo cambio.
    public boolean actualizar(Repuesto repuesto) {
        if (repuesto != null && repuesto.getId() <= 0) {
            throw new ValidacionException("El id del repuesto debe ser mayor que 0.");
        }
        validar(repuesto);
        boolean actualizado = enTransaccion(con -> dao.actualizar(con, repuesto));
        if (actualizado) publicador.publicar(repuesto.getId());
        return actualizado;
    }

    // Validar ID -> DAO elimina -> commit -> Observer, si se eliminó realmente.
    public boolean eliminar(int id) {
        if (id <= 0) {
            throw new ValidacionException("El id del repuesto debe ser mayor que 0.");
        }
        boolean eliminado = enTransaccion(con -> dao.eliminar(con, id));
        if (eliminado) publicador.publicar(id);
        return eliminado;
    }

    // ---------- Consultas ----------

    // CONSULTA: leer() abre/cierra conexión; DAO hace SELECT; retorna la lista.
    public List<Repuesto> listar() {
        return leer(con -> dao.listar(con));
    }

    public List<Repuesto> buscarPorId(int id) {
        return leer(con -> dao.buscarPorId(con, id));
    }

    public List<Repuesto> buscarPorNombre(String texto) {
        String buscado = texto == null ? "" : texto.trim();
        return leer(con -> dao.buscarPorNombre(con, buscado));
    }

    public List<Repuesto> listarPorTipo(int tipoId) {
        return leer(con -> dao.listarPorTipo(con, tipoId));
    }

    public List<Repuesto> listarPorMarca(int marcaId) {
        return leer(con -> dao.listarPorMarca(con, marcaId));
    }

    public List<Repuesto> listarPorProveedor(int proveedorId) {
        return leer(con -> dao.listarPorProveedor(con, proveedorId));
    }

    public List<Repuesto> listarStockBajo() {
        return leer(con -> dao.listarStockBajo(con));
    }

    // ---------- Validaciones ----------

    // Reglas de negocio: verificar datos antes de escribir en MySQL.
    // RepuestoController convierte tipos, pero el Service decide si son válidos.
    private void validar(Repuesto r) {
        if (r == null) {
            throw new ValidacionException("El repuesto no puede ser nulo.");
        }
        if (r.getNombre() == null || r.getNombre().isBlank()) {
            throw new ValidacionException("El nombre es obligatorio.");
        }
        if (r.getNombre().trim().length() > NOMBRE_MAXIMO) {
            throw new ValidacionException("El nombre no puede pasar de " + NOMBRE_MAXIMO + " caracteres.");
        }
        if (r.getTipoId() <= 0) {
            throw new ValidacionException("El tipo de repuesto es obligatorio.");
        }
        if (r.getMarcaId() <= 0) {
            throw new ValidacionException("La marca es obligatoria.");
        }
        if (r.getProveedorId() <= 0) {
            throw new ValidacionException("El proveedor es obligatorio.");
        }
        if (!proveedores.existe(r.getProveedorId())) {
            throw new ValidacionException("El proveedor " + r.getProveedorId() + " no existe.");
        }
        validarPrecio("El precio de compra", r.getPrecioCompra());
        validarPrecio("El precio de venta", r.getPrecioVenta());
        if (r.getStockMinimo() < 0) {
            throw new ValidacionException("El stock mínimo no puede ser negativo.");
        }
        // Dejar los datos listos para guardar
        r.setNombre(r.getNombre().trim());
        r.setPrecioCompra(r.getPrecioCompra().setScale(2));
        r.setPrecioVenta(r.getPrecioVenta().setScale(2));
    }

    private void validarPrecio(String campo, BigDecimal precio) {
        if (precio == null) {
            throw new ValidacionException(campo + " es obligatorio.");
        }
        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException(campo + " no puede ser negativo.");
        }
        if (precio.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException(campo + " puede tener máximo 2 decimales.");
        }
        if (precio.compareTo(PRECIO_MAXIMO) > 0) {
            throw new ValidacionException(campo + " no puede pasar de " + PRECIO_MAXIMO.toPlainString() + ".");
        }
    }

    // ---------- Manejo de la conexión ----------

    /** Abre la conexión, ejecuta la acción y la cierra. Para consultas. */
    // La lambda recibida es la operación DAO que queremos consultar.
    // No se hace commit porque aquí no se escriben datos.
    private <T> T leer(Function<Connection, T> accion) {
        try (Connection con = conexiones.get()) {
            return accion.apply(con);
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión.", e);
        }
    }

    /** Igual, pero con commit si todo sale bien y rollback si algo falla. Para cambios. */
    // Recibe la lambda con la operación DAO, inicia transacción y confirma.
    // Si falla: rollback. El Observer se llama después, fuera de este método.
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