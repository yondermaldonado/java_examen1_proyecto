
package com.carmotors.dao;

import com.carmotors.modelo.Cliente;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ClienteDAO {

    // CREATE
    Cliente insertar(Connection conexion, Cliente cliente);

    // READ
    Optional<Cliente> buscarPorId(Connection conexion, int id);

    Optional<Cliente> buscarPorDocumento(
            Connection conexion, String documento
    );

    Optional<Cliente> buscarPorEmail(
            Connection conexion, String email
    );

    List<Cliente> listar(Connection conexion);

    // UPDATE
    boolean actualizar(Connection conexion, Cliente cliente);

    // DELETE
    boolean eliminar(Connection conexion, int id);
}
