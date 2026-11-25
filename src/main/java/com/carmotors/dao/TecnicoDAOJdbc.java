package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.Tecnico;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TecnicoDAOJdbc implements TecnicoDAO {
    private static final String COLUMNAS = "id, nombre, documento_identidad, telefono, especialidad, activo";

    @Override
    public int crear(Connection con, Tecnico tecnico) {
        String sql = "INSERT INTO tecnicos (nombre, documento_identidad, telefono, especialidad, activo) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            datos(ps, tecnico);
            ps.executeUpdate();
            try (ResultSet claves = ps.getGeneratedKeys()) {
                if (!claves.next()) throw new SQLException("No se obtuvo el identificador del técnico.");
                return claves.getInt(1);
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo crear el técnico.", e);
        }
    }

    @Override
    public Optional<Tecnico> buscarPorId(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM tecnicos WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo consultar el técnico.", e);
        }
    }

    @Override
    public List<Tecnico> listar(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM tecnicos ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            List<Tecnico> tecnicos = new ArrayList<>();
            while (rs.next()) tecnicos.add(mapear(rs));
            return tecnicos;
        } catch (SQLException e) {
            throw new DaoException("No se pudieron listar los técnicos.", e);
        }
    }

    @Override
    public boolean actualizar(Connection con, Tecnico tecnico) {
        String sql = "UPDATE tecnicos SET nombre = ?, documento_identidad = ?, telefono = ?, especialidad = ?, activo = ? WHERE id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            datos(ps, tecnico);
            ps.setInt(6, tecnico.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("No se pudo actualizar el técnico.", e);
        }
    }

    @Override
    public boolean eliminar(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("DELETE FROM tecnicos WHERE id = ?")) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("No se pudo eliminar el técnico; comprueba sus referencias.", e);
        }
    }

    @Override
    public Optional<Tecnico> buscarPorDocumento(Connection con, String documento) {
        try (PreparedStatement ps = con.prepareStatement("SELECT " + COLUMNAS + " FROM tecnicos WHERE documento_identidad = ?")) {
            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo buscar el técnico por documento.", e);
        }
    }

    @Override
    public boolean tieneOrdenes(Connection con, int id) {
        try (PreparedStatement ps = con.prepareStatement("SELECT 1 FROM servicios_tecnicos WHERE tecnico_id = ? LIMIT 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar las órdenes del técnico.", e);
        }
    }

    private void datos(PreparedStatement ps, Tecnico t) throws SQLException {
        ps.setString(1, t.getNombre());
        ps.setString(2, t.getDocumentoIdentidad());
        ps.setString(3, t.getTelefono());
        ps.setString(4, t.getEspecialidad());
        ps.setBoolean(5, t.isActivo());
    }

    private Tecnico mapear(ResultSet rs) throws SQLException {
        Tecnico t = new Tecnico();
        t.setId(rs.getInt("id"));
        t.setNombre(rs.getString("nombre"));
        t.setDocumentoIdentidad(rs.getString("documento_identidad"));
        t.setTelefono(rs.getString("telefono"));
        t.setEspecialidad(rs.getString("especialidad"));
        t.setActivo(rs.getBoolean("activo"));
        return t;
    }
}
