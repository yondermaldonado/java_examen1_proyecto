
package com.carmotors.controlador;

import com.carmotors.modelo.Vehiculo;
import com.carmotors.servicio.VehiculoService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class VehiculoController {

    private final VehiculoService vehiculoService;

    // CONSTRUCTOR: Inyección del servicio
    public VehiculoController(VehiculoService vehiculoService) {

        this.vehiculoService = Objects.requireNonNull(
                vehiculoService,
                "El servicio de vehículos no puede ser null"
        );
    }

    // =========================================
    // CREATE: REGISTRAR VEHÍCULO
    // =========================================

    public Vehiculo registrarVehiculo(
            String placa,
            int clienteId,
            int modeloId) {

        Vehiculo vehiculo = new Vehiculo(
                placa,
                clienteId,
                modeloId
        );

        return vehiculoService.registrar(vehiculo);
    }

    // =========================================
    // READ: OBTENER VEHÍCULO POR ID
    // =========================================

    public Vehiculo obtenerVehiculoPorId(int id) {

        return vehiculoService.obtenerPorId(id);
    }

    // =========================================
    // READ: BUSCAR VEHÍCULO POR PLACA
    // =========================================

    public Optional<Vehiculo> buscarVehiculoPorPlaca(
            String placa) {

        return vehiculoService.buscarPorPlaca(placa);
    }

    // =========================================
    // READ: LISTAR TODOS LOS VEHÍCULOS
    // =========================================

    public List<Vehiculo> listarVehiculos() {

        return vehiculoService.listar();
    }

    // =========================================
    // READ: LISTAR VEHÍCULOS DE UN CLIENTE
    // =========================================

    public List<Vehiculo> listarVehiculosPorCliente(
            int clienteId) {

        return vehiculoService.listarPorCliente(clienteId);
    }

    // =========================================
    // UPDATE: ACTUALIZAR VEHÍCULO
    // =========================================

    public Vehiculo actualizarVehiculo(
            int id,
            String placa,
            int clienteId,
            int modeloId) {

        Vehiculo vehiculo = new Vehiculo(
                placa,
                clienteId,
                modeloId
        );

        vehiculo.setId(id);

        return vehiculoService.actualizar(vehiculo);
    }

    // =========================================
    // DELETE: ELIMINAR VEHÍCULO
    // =========================================

    public void eliminarVehiculo(int id) {

        vehiculoService.eliminar(id);
    }
}
