package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Repuesto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

// DAO JDBC: recibe una Connection desde RepuestoService.
// Ejecuta SQL y convierte ResultSet en objetos Repuesto.
// No confirma transacciones: el commit/rollback pertenece a RepuestoService.
public class RepuestoDAOJdbc implements RepuestoDAO {

    // Consulta base del catálogo. Cada filtro le agrega un WHERE.
    // SELECT común: incluye tipo, marca y proveedor. Cada consulta agrega su WHERE.
    private static final String SQL_CATALOGO =
            "SELECT r.id AS id, r.nombre AS nombre, "
                    + "r.repuesto_tipo_id AS tipo_id, rt.nombre AS tipo, "
                    + "r.marca_id AS marca_id, mr.nombre AS marca, "
                    + "pr.proveedor_id AS proveedor_id, p.nombre AS proveedor, "
                    + "pr.costo AS precio_compra, r.precio_venta AS precio_venta, "
                    + "r.stock_minimo AS stock_minimo "
                    + "FROM repuestos AS r "
                    + "INNER JOIN repuesto_tipos AS rt ON rt.id = r.repuesto_tipo_id "
                    + "INNER JOIN marcas_repuesto AS mr ON mr.id = r.marca_id "
                    + "LEFT JOIN proveedores_repuestos AS pr ON pr.repuesto_id = r.id "
                    + "LEFT JOIN proveedores AS p ON p.id = pr.proveedor_id ";
    private static final String ORDEN = " ORDER BY r.id, pr.proveedor_id";

    // ---------- Consultas ----------

    @Override
    public List<Repuesto> listar(Connection con) {
        return consultar(con, "");
    }

    @Override
    public List<Repuesto> buscarPorId(Connection con, int id) {
        return consultar(con, "WHERE r.id = ?", id);
    }

    @Override
    public List<Repuesto> buscarPorNombre(Connection con, String texto) {
        return consultar(con, "WHERE r.nombre LIKE ?", "%" + texto + "%");
    }

    @Override
    public List<Repuesto> listarPorTipo(Connection con, int tipoId) {
        return consultar(con, "WHERE r.repuesto_tipo_id = ?", tipoId);
    }

    @Override
    public List<Repuesto> listarPorMarca(Connection con, int marcaId) {
        return consultar(con, "WHERE r.marca_id = ?", marcaId);
    }

    @Override
    public List<Repuesto> listarPorProveedor(Connection con, int proveedorId) {
        return consultar(con, "WHERE pr.proveedor_id = ?", proveedorId);
    }

    @Override
    public List<Repuesto> listarStockBajo(Connection con) {
        return consultar(con, "WHERE (SELECT COALESCE(SUM(lr.cantidad_actual), 0) "
                + "FROM lotes_repuestos AS lr WHERE lr.repuesto_id = r.id) < r.stock_minimo");
    }

    // 1. Construir SELECT. 2. Poner parámetros. 3. Ejecutar SELECT.
    // 4. Recorrer filas. 5. Convertir cada fila a Repuesto con mapear().
    private List<Repuesto> consultar(Connection con, String condicion, Object... parametros) {
        String sql = SQL_CATALOGO + condicion + ORDEN;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Repuesto> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar los repuestos.", e);
        }
    }

    // Convierte UNA fila del ResultSet en UN objeto Repuesto.
    private Repuesto mapear(ResultSet rs) throws SQLException {
        Repuesto r = new Repuesto();
        r.setId(rs.getInt("id"));
        r.setNombre(rs.getString("nombre"));
        r.setTipoId(rs.getInt("tipo_id"));
        r.setTipo(rs.getString("tipo"));
        r.setMarcaId(rs.getInt("marca_id"));
        r.setMarca(rs.getString("marca"));
        r.setProveedorId(rs.getInt("proveedor_id"));   // 0 si el repuesto no tiene proveedor
        r.setProveedor(rs.getString("proveedor"));
        r.setPrecioCompra(rs.getBigDecimal("precio_compra"));
        r.setPrecioVenta(rs.getBigDecimal("precio_venta"));
        r.setStockMinimo(rs.getInt("stock_minimo"));
        return r;
    }

    // ---------- Cambios (dos tablas: el servicio los envuelve en una transacción) ----------

    @Override
    // Guarda en repuestos y proveedores_repuestos usando la misma Connection.
    // RepuestoService será quien confirme o revierta los cambios.
    public int crear(Connection con, Repuesto r) {
        String sqlRepuesto = "INSERT INTO repuestos "
                + "(nombre, repuesto_tipo_id, marca_id, precio_venta, stock_minimo) VALUES (?, ?, ?, ?, ?)";
        String sqlVinculo = "INSERT INTO proveedores_repuestos (proveedor_id, repuesto_id, costo) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlRepuesto, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getNombre());
            ps.setInt(2, r.getTipoId());
            ps.setInt(3, r.getMarcaId());
            ps.setBigDecimal(4, r.getPrecioVenta());
            ps.setInt(5, r.getStockMinimo());
            ps.executeUpdate();
            int id;
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                id = claves.getInt(1);
            }
            try (PreparedStatement pv = con.prepareStatement(sqlVinculo)) {
                pv.setInt(1, r.getProveedorId());
                pv.setInt(2, id);
                pv.setBigDecimal(3, r.getPrecioCompra());
                pv.executeUpdate();
            }
            return id;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo crear el repuesto.");
        }
    }

    @Override
    public boolean actualizar(Connection con, Repuesto r) {
        String sqlRepuesto = "UPDATE repuestos SET nombre = ?, repuesto_tipo_id = ?, marca_id = ?, "
                + "precio_venta = ?, stock_minimo = ? WHERE id = ?";
        String sqlCosto = "UPDATE proveedores_repuestos SET costo = ? WHERE proveedor_id = ? AND repuesto_id = ?";
        String sqlVinculo = "INSERT INTO proveedores_repuestos (proveedor_id, repuesto_id, costo) VALUES (?, ?, ?)";
        try {
            int filas;
            try (PreparedStatement ps = con.prepareStatement(sqlRepuesto)) {
                ps.setString(1, r.getNombre());
                ps.setInt(2, r.getTipoId());
                ps.setInt(3, r.getMarcaId());
                ps.setBigDecimal(4, r.getPrecioVenta());
                ps.setInt(5, r.getStockMinimo());
                ps.setInt(6, r.getId());
                filas = ps.executeUpdate();
            }
            if (filas == 0) {
                return false; // el repuesto no existe
            }
            int costoActualizado;
            try (PreparedStatement pc = con.prepareStatement(sqlCosto)) {
                pc.setBigDecimal(1, r.getPrecioCompra());
                pc.setInt(2, r.getProveedorId());
                pc.setInt(3, r.getId());
                costoActualizado = pc.executeUpdate();
            }
            if (costoActualizado == 0) {   // ese proveedor aún no vendía este repuesto
                try (PreparedStatement pv = con.prepareStatement(sqlVinculo)) {
                    pv.setInt(1, r.getProveedorId());
                    pv.setInt(2, r.getId());
                    pv.setBigDecimal(3, r.getPrecioCompra());
                    pv.executeUpdate();
                }
            }
            return true;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo actualizar el repuesto.");
        }
    }

    @Override
    public boolean eliminar(Connection con, int id) {
        String sqlVinculos = "DELETE FROM proveedores_repuestos WHERE repuesto_id = ?";
        String sqlRepuesto = "DELETE FROM repuestos WHERE id = ?";
        try {
            try (PreparedStatement pv = con.prepareStatement(sqlVinculos)) {
                pv.setInt(1, id);
                pv.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(sqlRepuesto)) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw traducir(e, "No se pudo eliminar el repuesto.");
        }
    }

    /** Convierte los errores conocidos de MySQL en mensajes claros. */
    private RuntimeException traducir(SQLException e, String mensajeTecnico) {
        switch (e.getErrorCode()) {
            case 1451:
                return new ValidacionException("No se puede eliminar: el repuesto tiene lotes o servicios asociados.");
            case 1452:
                return new ValidacionException("El tipo, la marca o el proveedor indicado no existe.");
            case 3819:
                return new ValidacionException("Un valor no cumple las reglas de la base de datos (por ejemplo, un precio negativo).");
            case 1264:
                return new ValidacionException("Un número está fuera del rango permitido.");
            case 1406:
                return new ValidacionException("Un texto es demasiado largo.");
            default:
                return new DaoException(mensajeTecnico, e);
        }
    }
}