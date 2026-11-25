
package com.carmotors.servicio;

import com.carmotors.modelo.ServicioHistorial;
import com.carmotors.modelo.Vehiculo;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HistorialService {

    private final VehiculoService vehiculoService;
    private final HistorialServiciosConsulta historialConsulta;

    // Inyeccion de dependencias
    public HistorialService(
            VehiculoService vehiculoService,
            HistorialServiciosConsulta historialConsulta) {

        this.vehiculoService = Objects.requireNonNull(
                vehiculoService,
                "El servicio de vehiculos es obligatorio"
        );

        this.historialConsulta = Objects.requireNonNull(
                historialConsulta,
                "La consulta del historial es obligatoria"
        );
    }

    // Obtener el historial de un vehiculo
    public List<ServicioHistorial> listarPorVehiculo(int vehiculoId) {

        validarId(vehiculoId);

        // Comprueba que el vehiculo exista.
        vehiculoService.obtenerPorId(vehiculoId);

        return consultarServicios(vehiculoId);
    }

    // Obtener los historiales de todos los vehiculos de un cliente
    public Map<Integer, List<ServicioHistorial>> listarPorCliente(
            int clienteId) {

        validarId(clienteId);

        List<Vehiculo> vehiculos =
                vehiculoService.listarPorCliente(clienteId);

        Map<Integer, List<ServicioHistorial>> resultado =
                new LinkedHashMap<>();

        for (Vehiculo vehiculo : vehiculos) {

            int vehiculoId = vehiculo.getId();

            List<ServicioHistorial> servicios =
                    consultarServicios(vehiculoId);

            // Incluye el vehiculo aunque su historial este vacio.
            resultado.put(vehiculoId, servicios);
        }

        return Collections.unmodifiableMap(resultado);
    }

    // Consultar y ordenar los servicios desde el mas reciente
    private List<ServicioHistorial> consultarServicios(int vehiculoId) {

        List<ServicioHistorial> servicios = Objects.requireNonNull(
                historialConsulta.listarPorVehiculo(vehiculoId),
                "La consulta no debe devolver null"
        );

        for (ServicioHistorial servicio : servicios) {

            if (servicio == null
                    || servicio.getVehiculoId() != vehiculoId) {

                throw new IllegalStateException(
                        "El historial contiene datos de otro vehiculo"
                );
            }
        }

        return servicios.stream()
                .sorted(
                        Comparator.comparing(
                                ServicioHistorial::getFechaRecepcion
                        ).reversed()
                )
                .toList();
    }

    private void validarId(int id) {

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor que cero"
            );
        }
    }
}
