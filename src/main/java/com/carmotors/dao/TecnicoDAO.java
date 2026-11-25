package com.carmotors.dao;

import com.carmotors.modelo.Tecnico;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** El llamador es propietario de la conexión y de la transacción. */
public interface TecnicoDAO {
    int crear(Connection conexion, Tecnico tecnico);
    Optional<Tecnico> buscarPorId(Connection conexion, int id);
    Optional<Tecnico> buscarPorDocumento(Connection conexion, String documento);
    boolean tieneOrdenes(Connection conexion, int id);
    List<Tecnico> listar(Connection conexion);
    boolean actualizar(Connection conexion, Tecnico tecnico);
    boolean eliminar(Connection conexion, int id);
}
