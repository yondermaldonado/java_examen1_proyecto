
package com.carmotors.controlador;

import com.carmotors.modelo.Proveedor;
import com.carmotors.servicio.ProveedorService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProveedorController {

    private final ProveedorService proveedorService;

    // =========================================
    // CONSTRUCTOR: INYECCION DE DEPENDENCIAS
    // =========================================

    public ProveedorController(
            ProveedorService proveedorService) {

        this.proveedorService = Objects.requireNonNull(
                proveedorService,
                "El servicio de proveedores no puede ser null"
        );
    }

    // =========================================
    // CREATE: REGISTRAR PROVEEDOR
    // =========================================

    public Proveedor registrarProveedor(
            String nombre,
            String contacto,
            String email) {

        Proveedor proveedor = new Proveedor(
                nombre,
                contacto,
                email
        );

        return proveedorService.registrar(proveedor);
    }

    // =========================================
    // READ: BUSCAR PROVEEDOR POR ID
    // =========================================

    public Optional<Proveedor> buscarProveedorPorId(int id) {

        return proveedorService.buscarPorId(id);
    }

    // =========================================
    // READ: BUSCAR POR NOMBRE
    // =========================================

    public List<Proveedor> buscarProveedoresPorNombre(
            String nombre) {

        return proveedorService.buscarPorNombre(nombre);
    }

    // =========================================
    // READ: BUSCAR POR EMAIL
    // =========================================

    public List<Proveedor> buscarProveedoresPorEmail(
            String email) {

        return proveedorService.buscarPorEmail(email);
    }

    // =========================================
    // READ: LISTAR TODOS LOS PROVEEDORES
    // =========================================

    public List<Proveedor> listarProveedores() {

        return proveedorService.listar();
    }

    // =========================================
    // READ: VERIFICAR EXISTENCIA
    // =========================================

    public boolean existeProveedor(int id) {

        return proveedorService.existe(id);
    }

    // =========================================
    // UPDATE: ACTUALIZAR PROVEEDOR
    // =========================================

    public boolean actualizarProveedor(
            int id,
            String nombre,
            String contacto,
            String email) {

        return proveedorService.actualizar(
                id,
                nombre,
                contacto,
                email
        );
    }

    // =========================================
    // DELETE: ELIMINAR PROVEEDOR
    // =========================================

    public boolean eliminarProveedor(int id) {

        return proveedorService.eliminar(id);
    }
}
