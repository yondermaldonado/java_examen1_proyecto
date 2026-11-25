
package com.carmotors.dao;

import com.carmotors.modelo.Proveedor;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProveedorDAO {

    // CREATE
    Proveedor insertar(Connection conexion, Proveedor proveedor);

    // READ
    Optional<Proveedor> buscarPorId(Connection conexion, int id);

    List<Proveedor> buscarPorNombre(
            Connection conexion, String nombre
    );

    List<Proveedor> buscarPorEmail(
            Connection conexion, String email
    );

    List<Proveedor> listar(Connection conexion);

    boolean existe(Connection conexion, int id);

    // UPDATE
    boolean actualizar(Connection conexion, Proveedor proveedor);

    // DELETE
    boolean eliminar(Connection conexion, int id);
}
