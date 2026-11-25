
package com.carmotors.controlador;

import com.carmotors.modelo.RecordatorioPreventivo;
import com.carmotors.modelo.ServicioHistorial;
import com.carmotors.servicio.HistorialService;
import com.carmotors.servicio.RecordatorioPreventivoService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HistorialController {

    private final HistorialService historialService;
    private final RecordatorioPreventivoService recordatorioService;

    public HistorialController(
            HistorialService historialService,
            RecordatorioPreventivoService recordatorioService) {

        this.historialService = Objects.requireNonNull(
                historialService,
                "El servicio de historial es obligatorio"
        );

        this.recordatorioService = Objects.requireNonNull(
                recordatorioService,
                "El servicio de recordatorios es obligatorio"
        );
    }

    // Consultar los servicios anteriores de un vehiculo.
    public List<ServicioHistorial> consultarHistorialVehiculo(
            int vehiculoId) {

        return historialService.listarPorVehiculo(vehiculoId);
    }

    // Consultar el historial de todos los vehiculos de un cliente.
    public Map<Integer, List<ServicioHistorial>> consultarHistorialCliente(
            int clienteId) {

        return historialService.listarPorCliente(clienteId);
    }

    // Consultar los recordatorios preventivos de un vehiculo.
    public List<RecordatorioPreventivo> consultarRecordatoriosVehiculo(
            int vehiculoId) {

        return recordatorioService.listarPorVehiculo(vehiculoId);
    }

    // Consultar los recordatorios de todos los vehiculos de un cliente.
    public Map<Integer, List<RecordatorioPreventivo>> consultarRecordatoriosCliente(
            int clienteId) {

        return recordatorioService.listarPorCliente(clienteId);
    }
}
