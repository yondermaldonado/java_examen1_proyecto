package com.carmotors.dao;

import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO JDBC: única clase de este módulo que escribe instrucciones SQL.
 * La llama RepuestoTipoService y recibe su Connection.
 * NO abre ni cierra la conexión ni confirma transacciones.
 */
public final class RepuestoTipoDAOJdbc implements RepuestoTipoDAO {

    // INSERT: guarda el nombre y devuelve el ID generado por MySQL.
    @Override
    public int crear(Connection conexion, String nombre) {
        String sql = "INSERT INTO repuesto_tipos (nombre) VALUES (?)";

        try (PreparedStatement consulta = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            consulta.setString(1, nombre);
            consulta.executeUpdate();

            try (ResultSet resultado = consulta.getGeneratedKeys()) {
                if (!resultado.next()) {
                    throw new DaoException("MySQL no devolvió el id del tipo creado.");
                }
                return resultado.getInt(1);
            }
        } catch (SQLException e) {
            throw traducir(e, "No se pudo crear el tipo de repuesto.");
        }
    }

    // UPDATE: modifica el nombre del tipo indicado.
    @Override
    public boolean actualizar(Connection conexion, int id, String nombre) {
        String sql = "UPDATE repuesto_tipos SET nombre = ? WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, nombre);
            consulta.setInt(2, id);

            int filasAfectadas = consulta.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo actualizar el tipo de repuesto.");
        }
    }

    // DELETE: intenta borrar el tipo. Si tiene repuestos asociados,
    // MySQL rechaza el borrado y traducir() prepara el mensaje.
    @Override
    public boolean eliminar(Connection conexion, int id) {
        String sql = "DELETE FROM repuesto_tipos WHERE id = ?";

        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setInt(1, id);
            int filasAfectadas = consulta.executeUpdate();
            return filasAfectadas > 0;
        } catch (SQLException e) {
            throw traducir(e, "No se pudo eliminar el tipo de repuesto.");
        }
    }

    // SELECT: devuelve todos los tipos ordenados por ID.
    @Override
    public List<RepuestoTipo> listar(Connection conexion) {
        String sql = "SELECT id, nombre FROM repuesto_tipos ORDER BY id";
        return consultar(conexion, sql, null);
    }

    // SELECT: consulta un ID y devuelve Optional.empty() si no existe.
    @Override
    public Optional<RepuestoTipo> buscarPorId(Connection conexion, int id) {
        String sql = "SELECT id, nombre FROM repuesto_tipos WHERE id = ?";
        List<RepuestoTipo> tipos = consultar(conexion, sql, id);

        if (tipos.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(tipos.get(0));
    }

    // SELECT LIKE: busca coincidencias parciales por nombre.
    @Override
    public List<RepuestoTipo> buscarPorNombre(Connection conexion, String nombre) {
        String sql = "SELECT id, nombre FROM repuesto_tipos WHERE nombre LIKE ? ORDER BY nombre";
        String nombreBuscado = "%" + nombre + "%";
        return consultar(conexion, sql, nombreBuscado);
    }

    // Método compartido por las tres consultas SELECT.
    // 1. Preparar SQL. 2. Asignar parámetro opcional.
    // 3. Ejecutar consulta. 4. Convertir cada fila en RepuestoTipo.
    private List<RepuestoTipo> consultar(Connection conexion, String sql, Object parametro) {
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            if (parametro != null) {
                consulta.setObject(1, parametro);
            }

            try (ResultSet resultado = consulta.executeQuery()) {
                List<RepuestoTipo> tipos = new ArrayList<>();

                while (resultado.next()) {
                    int id = resultado.getInt("id");
                    String nombre = resultado.getString("nombre");
                    RepuestoTipo tipo = new RepuestoTipo(id, nombre);
                    tipos.add(tipo);
                }
                return tipos;
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudieron consultar los tipos de repuesto.", e);
        }
    }

    // Traduce códigos de error MySQL en errores comprensibles.
    private RuntimeException traducir(SQLException error, String mensaje) {
        int codigo = error.getErrorCode();

        if (codigo == 1062) {
            return new ValidacionException("Ya existe un tipo con ese nombre.");
        }
        if (codigo == 1451) {
            return new ValidacionException("No se puede eliminar: hay repuestos asociados a este tipo.");
        }
        if (codigo == 1406) {
            return new ValidacionException("El nombre del tipo excede los 100 caracteres.");
        }
        return new DaoException(mensaje, error);
    }
}
