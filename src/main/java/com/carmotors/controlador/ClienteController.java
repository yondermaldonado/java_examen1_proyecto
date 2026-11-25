
package com.carmotors.controlador;

import com.carmotors.servicio.ClienteService;
import com.carmotors.modelo.Cliente;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = Objects.requireNonNull(
                clienteService,
                "El servicio de clientes no puede ser null"
        );
    }

    // =========================================
    // CREATE: Registrar Clientes
    // =========================================

    public Cliente registrarCliente(
            String nombre,
            String documento,
            String telefono,
            String email) {

        // Construir el cliente con los datos recibidos.
        Cliente cliente = new Cliente(
                nombre,
                documento,
                telefono,
                email
        );

        // Delegar el registro al servicio.
        return clienteService.registrar(cliente);
    }


    // =========================================
    // READ: OBTENER CLIENTE POR ID
    // =========================================

    public Cliente obtenerClientePorId(int id) {

        return clienteService.obtenerPorId(id);
    }


    // =========================================
    // READ: BUSCAR POR DOCUMENTO
    // =========================================

    public Optional<Cliente> buscarClientePorDocumento(
            String documento) {

        return clienteService.buscarPorDocumento(documento);
    }


    // =========================================
    // READ: BUSCAR POR EMAIL
    // =========================================

    public Optional<Cliente> buscarClientePorEmail(
            String email) {

        return clienteService.buscarPorEmail(email);
    }


    // =========================================
    // READ: LISTAR TODOS LOS CLIENTES
    // =========================================

    public List<Cliente> listarClientes() {

        return clienteService.listar();
    }


    // =========================================
    // UPDATE: ACTUALIZAR CLIENTE
    // =========================================

    public Cliente actualizarCliente(
            int id,
            String nombre,
            String documento,
            String telefono,
            String email) {

        // Construir un cliente con los nuevos datos.
        Cliente cliente = new Cliente(
                nombre,
                documento,
                telefono,
                email
        );

        // Identificar qué registro se actualizará.
        cliente.setId(id);

        // Delegar las reglas de negocio al servicio.
        return clienteService.actualizar(cliente);
    }


    // =========================================
    // DELETE: ELIMINAR CLIENTE
    // =========================================

    public void eliminarCliente(int id) {

        clienteService.eliminar(id);
    }


}
