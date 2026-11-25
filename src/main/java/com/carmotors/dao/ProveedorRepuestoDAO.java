
package com.carmotors.dao;

import com.carmotors.modelo.ProveedorRepuesto;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface ProveedorRepuestoDAO {

    // CREATE
    void insertar(
            Connection conexion,
            ProveedorRepuesto asociacion
    );

    // READ
    Optional<ProveedorRepuesto> buscar(
            Connection conexion,
            int proveedorId,
            int repuestoId
    );

    List<ProveedorRepuesto> listarPorProveedor(
            Connection conexion,
            int proveedorId
    );

    List<ProveedorRepuesto> listarPorRepuesto(
            Connection conexion,
            int repuestoId
    );

    // UPDATE
    boolean actualizarCosto(
            Connection conexion,
            ProveedorRepuesto asociacion
    );
}
