package com.carmotors.dao;

import com.carmotors.modelo.RepuestoTipo;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * DAO: define qué consultas puede solicitar RepuestoTipoService.
 * Esta interfaz NO contiene SQL ni valida reglas de negocio.
 * La conexión la abre y cierra el Service; el DAO no hace commit.
 */
public interface RepuestoTipoDAO {

    // Inserta un tipo nuevo y devuelve su ID generado.
    int crear(Connection conexion, String nombre);

    // Devuelve true si se actualizó un registro.
    boolean actualizar(Connection conexion, int id, String nombre);

    // Devuelve true si se eliminó un registro.
    boolean eliminar(Connection conexion, int id);

    // Consulta todos los tipos de repuesto.
    List<RepuestoTipo> listar(Connection conexion);

    // Busca un tipo por ID; puede no existir.
    Optional<RepuestoTipo> buscarPorId(Connection conexion, int id);

    // Busca tipos cuyo nombre contiene el texto recibido.
    List<RepuestoTipo> buscarPorNombre(Connection conexion, String nombre);
}
