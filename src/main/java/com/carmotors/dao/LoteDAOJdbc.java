package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.Lote;
import com.carmotors.modelo.LoteLinea;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO JDBC: implementa el contrato LoteDAO.
 * Solo trabaja con SQL; no valida reglas del negocio ni hace commit.
 * La conexion la recibe desde LoteService.
 */
public class LoteDAOJdbc implements LoteDAO {

    // Consulta base de trazabilidad: una fila por (lote, repuesto). Cada filtro le agrega un WHERE.
    private static final String SQL_EXISTENCIAS =
            "SELECT l.id AS lote_id, l.codigo AS codigo, l.proveedor_id AS proveedor_id, p.nombre AS proveedor, "
                    + "l.fecha_ingreso AS fecha_ingreso, lr.repuesto_id AS repuesto_id, r.nombre AS repuesto, "
                    + "lr.cantidad_ingresada AS cantidad_ingresada, lr.cantidad_actual AS cantidad_actual, "
                    + "lr.cantidad_actual - COALESCE((SELECT SUM(rr.cantidad) FROM reservas_repuestos AS rr "
                    + "INNER JOIN reserva_estados AS re ON re.id = rr.estado_id "
                    + "WHERE rr.lote_id = lr.lote_id AND rr.repuesto_id = lr.repuesto_id AND re.nombre = 'ACTIVA'), 0) "
                    + "AS cantidad_disponible, "
                    + "lr.costo_unitario AS costo_unitario, lr.fecha_caducidad AS fecha_caducidad "
                    + "FROM lotes_repuestos AS lr "
                    + "INNER JOIN lotes AS l ON l.id = lr.lote_id "
                    + "INNER JOIN proveedores AS p ON p.id = l.proveedor_id "
                    + "INNER JOIN repuestos AS r ON r.id = lr.repuesto_id ";
    private static final String ORDEN = " ORDER BY l.fecha_ingreso, l.id, lr.repuesto_id";

    // ---------- Consultas ----------

    @Override
    public List<ExistenciaLote> buscarPorCodigo(Connection con, String codigo) {
        return consultar(con, "WHERE l.codigo = ?", codigo);
    }

    @Override
    public List<ExistenciaLote> listarPorRepuesto(Connection con, int repuestoId) {
        return consultar(con, "WHERE lr.repuesto_id = ?", repuestoId);
    }

    @Override
    public List<ExistenciaLote> listarPorProveedor(Connection con, int proveedorId) {
        return consultar(con, "WHERE l.proveedor_id = ?", proveedorId);
    }

    /** Ejecuta una consulta y transforma cada fila en ExistenciaLote. */
    private List<ExistenciaLote> consultar(Connection con, String condicion, Object parametro) {
        String sql = SQL_EXISTENCIAS + condicion + ORDEN;
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setObject(1, parametro);
            try (ResultSet rs = ps.executeQuery()) {
                List<ExistenciaLote> lista = new ArrayList<>();
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
                return lista;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar los lotes.", e);
        }
    }

    /** Lee una fila del ResultSet y construye el modelo de trazabilidad. */
    private ExistenciaLote mapear(ResultSet rs) throws SQLException {
        return new ExistenciaLote(
                rs.getInt("lote_id"), rs.getString("codigo"),
                rs.getInt("proveedor_id"), rs.getString("proveedor"),
                rs.getDate("fecha_ingreso").toLocalDate(),
                rs.getInt("repuesto_id"), rs.getString("repuesto"),
                rs.getInt("cantidad_ingresada"), rs.getInt("cantidad_actual"), rs.getInt("cantidad_disponible"),
                rs.getBigDecimal("costo_unitario"), aFecha(rs.getDate("fecha_caducidad")));
    }

    private LocalDate aFecha(Date fecha) {
        return fecha == null ? null : fecha.toLocalDate();
    }

    @Override
    public int stockFisico(Connection con, int repuestoId) {
        String sql = "SELECT COALESCE(SUM(lr.cantidad_actual), 0) AS total "
                + "FROM lotes_repuestos AS lr WHERE lr.repuesto_id = ?";
        return sumar(con, sql, repuestoId, "No se pudo consultar el stock físico.");
    }

    @Override
    public int stockReservado(Connection con, int repuestoId) {
        String sql = "SELECT COALESCE(SUM(rr.cantidad), 0) AS total FROM reservas_repuestos AS rr "
                + "INNER JOIN reserva_estados AS re ON re.id = rr.estado_id "
                + "WHERE rr.repuesto_id = ? AND re.nombre = 'ACTIVA'";
        return sumar(con, sql, repuestoId, "No se pudo consultar las reservas activas.");
    }

    /** Ejecuta el SUM de SQL que reciben las consultas de stock. */
    private int sumar(Connection con, String sql, int repuestoId, String mensajeError) {
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, repuestoId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            throw new DaoException(mensajeError, e);
        }
    }

    // ---------- Ingreso (dos tablas: el servicio lo envuelve en una transacción) ----------

    @Override
    /**
     * PASO 1: inserta cabecera en lotes.
     * PASO 2: obtiene el ID que genero MySQL.
     * PASO 3: inserta cada repuesto en lotes_repuestos.
     * LoteService confirma o revierte ambas tablas en una transaccion.
     */
    public int crear(Connection con, Lote lote) {
        String sqlLote = "INSERT INTO lotes (codigo, proveedor_id, fecha_ingreso) VALUES (?, ?, ?)";
        String sqlLinea = "INSERT INTO lotes_repuestos "
                + "(lote_id, repuesto_id, cantidad_ingresada, cantidad_actual, costo_unitario, fecha_caducidad) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sqlLote, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, lote.getCodigo());
            ps.setInt(2, lote.getProveedorId());
            ps.setDate(3, Date.valueOf(lote.getFechaIngreso()));
            // Guardar la cabecera del ingreso.
            ps.executeUpdate();
            int loteId;
            try (ResultSet claves = ps.getGeneratedKeys()) {
                claves.next();
                loteId = claves.getInt(1);
            }
            // Guardar uno por uno los repuestos asociados al lote creado.
            try (PreparedStatement pl = con.prepareStatement(sqlLinea)) {
                for (LoteLinea linea : lote.getLineas()) {
                    pl.setInt(1, loteId);
                    pl.setInt(2, linea.getRepuestoId());
                    pl.setInt(3, linea.getCantidadIngresada());
                    pl.setInt(4, linea.getCantidadIngresada()); // al ingresar, lo actual es lo que llegó
                    pl.setBigDecimal(5, linea.getCostoUnitario());
                    if (linea.getFechaCaducidad() == null) {
                        pl.setNull(6, Types.DATE);
                    } else {
                        pl.setDate(6, Date.valueOf(linea.getFechaCaducidad()));
                    }
                    pl.executeUpdate();
                }
            }
            return loteId;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo registrar el lote.");
        }
    }

    /** Convierte los errores conocidos de MySQL en mensajes claros. */
    private RuntimeException traducir(SQLException e, String mensajeTecnico) {
        switch (e.getErrorCode()) {
            case 1062:
                if (e.getMessage() != null && e.getMessage().contains("uk_lotes_codigo")) {
                    return new ValidacionException("Ya existe un lote con ese código.");
                }
                return new ValidacionException("Registro duplicado: el repuesto ya está en ese lote.");
            case 1452:
                return new ValidacionException("El proveedor o el repuesto indicado no existe.");
            case 3819:
                return new ValidacionException("Un valor no cumple las reglas de la base de datos (por ejemplo, una cantidad inválida).");
            case 1264:
                return new ValidacionException("Un número está fuera del rango permitido.");
            case 1406:
                return new ValidacionException("Un texto es demasiado largo.");
            default:
                return new DaoException(mensajeTecnico, e);
        }
    }
}