package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.modelo.CargaTecnico;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServicioTecnicoDAOJdbc implements ServicioTecnicoDAO {
    @Override
    public Optional<String> bloquearEstadoOrden(Connection con, int servicioId) {
        String sql = "SELECT e.nombre FROM servicios s JOIN servicio_estados e ON e.id = s.estado_id WHERE s.id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, servicioId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DaoException("No se pudo bloquear la orden para asignación.", e); }
    }

    @Override
    public Optional<Boolean> bloquearTecnicoActivo(Connection con, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("SELECT activo FROM tecnicos WHERE id = ? FOR UPDATE")) {
            ps.setInt(1, tecnicoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBoolean(1)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DaoException("No se pudo bloquear el técnico para asignación.", e); }
    }

    @Override
    public boolean existe(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM servicios_tecnicos WHERE servicio_id = ? AND tecnico_id = ?")) {
            ids(ps, servicioId, tecnicoId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new DaoException("No se pudo consultar la asignación.", e); }
    }

    /** Se ejecuta con la orden bloqueada y en la transacción de la asignación. */
    @Override
    public void iniciarOrden(Connection con, int servicioId) {
        String sql = "UPDATE servicios s JOIN servicio_estados actual ON actual.id = s.estado_id "
                + "JOIN servicio_estados nuevo ON nuevo.nombre = 'En Proceso' "
                + "SET s.estado_id = nuevo.id WHERE s.id = ? AND actual.nombre = 'Pendiente'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, servicioId);
            int modificadas = ps.executeUpdate();
            if (modificadas == 0 && !bloquearEstadoOrden(con, servicioId).orElse("").equals("En Proceso")) {
                throw new DaoException("No se pudo iniciar la orden al asignar el técnico.");
            }
        } catch (SQLException e) { throw new DaoException("No se pudo iniciar la orden al asignar el técnico.", e); }
    }

    @Override
    public void asignar(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO servicios_tecnicos (servicio_id, tecnico_id, horas_trabajadas) VALUES (?, ?, 0.00)")) {
            ids(ps, servicioId, tecnicoId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new DaoException("No se pudo asignar el técnico.", e); }
    }

    @Override
    public boolean quitar(Connection con, int servicioId, int tecnicoId) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM servicios_tecnicos WHERE servicio_id = ? AND tecnico_id = ?")) {
            ids(ps, servicioId, tecnicoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudo quitar la asignación.", e); }
    }

    @Override
    public boolean actualizarHoras(Connection con, int servicioId, int tecnicoId, BigDecimal horas) {
        try (PreparedStatement ps = con.prepareStatement("UPDATE servicios_tecnicos SET horas_trabajadas = ? WHERE servicio_id = ? AND tecnico_id = ?")) {
            ps.setBigDecimal(1, horas); ps.setInt(2, servicioId); ps.setInt(3, tecnicoId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) { throw new DaoException("No se pudieron actualizar las horas del técnico.", e); }
    }

    @Override public List<ServicioTecnico> listarPorOrden(Connection con, int id) {
        return listar(con, "st.servicio_id", id);
    }
    @Override public List<ServicioTecnico> listarPorTecnico(Connection con, int id) {
        return listar(con, "st.tecnico_id", id);
    }

    private List<ServicioTecnico> listar(Connection con, String campo, int id) {
        String sql = "SELECT st.servicio_id, st.tecnico_id, t.nombre, e.nombre AS estado, st.horas_trabajadas "
                + "FROM servicios_tecnicos st JOIN tecnicos t ON t.id = st.tecnico_id "
                + "JOIN servicios s ON s.id = st.servicio_id JOIN servicio_estados e ON e.id = s.estado_id "
                + "WHERE " + campo + " = ? ORDER BY st.servicio_id, st.tecnico_id";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<ServicioTecnico> filas = new ArrayList<>();
                while (rs.next()) filas.add(new ServicioTecnico(rs.getInt("servicio_id"), rs.getInt("tecnico_id"),
                        rs.getString("nombre"), rs.getString("estado"), rs.getBigDecimal("horas_trabajadas")));
                return filas;
            }
        } catch (SQLException e) { throw new DaoException("No se pudieron consultar las asignaciones.", e); }
    }

    @Override
    public Optional<BigDecimal> bloquearHoras(Connection con, int servicioId, int tecnicoId) {
        String sql = "SELECT horas_trabajadas FROM servicios_tecnicos WHERE servicio_id = ? AND tecnico_id = ? FOR UPDATE";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ids(ps, servicioId, tecnicoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getBigDecimal(1)) : Optional.empty();
            }
        } catch (SQLException e) { throw new DaoException("No se pudo consultar el tiempo acumulado.", e); }
    }

    @Override
    public List<CargaTecnico> listarCargaEnProceso(Connection con) {
        String sql = "SELECT t.id, t.nombre, t.activo, COUNT(*) AS cantidad "
                + "FROM tecnicos t JOIN servicios_tecnicos st ON st.tecnico_id = t.id "
                + "JOIN servicios s ON s.id = st.servicio_id JOIN servicio_estados e ON e.id = s.estado_id "
                + "WHERE e.nombre = 'En Proceso' GROUP BY t.id, t.nombre, t.activo "
                + "ORDER BY cantidad DESC, t.nombre, t.id";
        return consultarCarga(con, sql);
    }

    @Override
    public List<CargaTecnico> listarSinOrdenesAbiertas(Connection con) {
        String sql = "SELECT t.id, t.nombre, t.activo, 0 AS cantidad FROM tecnicos t WHERE NOT EXISTS ("
                + "SELECT 1 FROM servicios_tecnicos st JOIN servicios s ON s.id = st.servicio_id "
                + "JOIN servicio_estados e ON e.id = s.estado_id WHERE st.tecnico_id = t.id "
                + "AND e.nombre IN ('Pendiente', 'En Proceso')) ORDER BY t.nombre, t.id";
        return consultarCarga(con, sql);
    }

    private List<CargaTecnico> consultarCarga(Connection con, String sql) {
        try (PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<CargaTecnico> filas = new ArrayList<>();
            while (rs.next()) {
                filas.add(new CargaTecnico(rs.getInt("id"), rs.getString("nombre"),
                        rs.getBoolean("activo"), rs.getLong("cantidad")));
            }
            return filas;
        } catch (SQLException e) { throw new DaoException("No se pudo consultar la carga de técnicos.", e); }
    }

    private void ids(PreparedStatement ps, int servicioId, int tecnicoId) throws SQLException {
        ps.setInt(1, servicioId); ps.setInt(2, tecnicoId);
    }
}
