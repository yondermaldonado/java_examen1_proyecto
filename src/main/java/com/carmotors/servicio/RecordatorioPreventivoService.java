
package com.carmotors.servicio;

import com.carmotors.modelo.OperacionHistorial;
import com.carmotors.modelo.RecordatorioPreventivo;
import com.carmotors.modelo.ServicioHistorial;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RecordatorioPreventivoService {

    private final HistorialService historialService;
    private final Clock clock;

    // CONSTRUCTOR
    public RecordatorioPreventivoService(
            HistorialService historialService,
            Clock clock) {

        this.historialService = Objects.requireNonNull(
                historialService,
                "El servicio de historial es obligatorio"
        );

        this.clock = Objects.requireNonNull(
                clock,
                "El reloj es obligatorio"
        );
    }

    // RECORDATORIOS DE UN VEHICULO
    public List<RecordatorioPreventivo> listarPorVehiculo(
            int vehiculoId) {

        List<ServicioHistorial> servicios =
                historialService.listarPorVehiculo(vehiculoId);

        return calcular(
                vehiculoId,
                servicios,
                LocalDate.now(clock)
        );
    }

    // RECORDATORIOS DE TODOS LOS VEHICULOS DE UN CLIENTE
    public Map<Integer, List<RecordatorioPreventivo>> listarPorCliente(
            int clienteId) {

        Map<Integer, List<ServicioHistorial>> historiales =
                historialService.listarPorCliente(clienteId);

        LocalDate hoy = LocalDate.now(clock);

        Map<Integer, List<RecordatorioPreventivo>> resultado =
                new LinkedHashMap<>();

        for (Map.Entry<Integer, List<ServicioHistorial>> entrada
                : historiales.entrySet()) {

            resultado.put(
                    entrada.getKey(),
                    calcular(
                            entrada.getKey(),
                            entrada.getValue(),
                            hoy
                    )
            );
        }

        return Collections.unmodifiableMap(resultado);
    }

    // LOGICA PRINCIPAL DE CALCULO
    private List<RecordatorioPreventivo> calcular(
            int vehiculoId,
            List<ServicioHistorial> servicios,
            LocalDate fechaConsulta) {

        // Clave: ID de la operacion del catalogo.
        // Valor: ultima vez que se realizo.
        Map<Integer, UltimaOperacion> ultimas =
                new HashMap<>();

        for (ServicioHistorial servicio : servicios) {

            LocalDateTime entrega =
                    servicio.getFechaEntrega();

            // No considerar servicios sin entrega o futuros.
            if (entrega == null
                    || entrega.toLocalDate().isAfter(fechaConsulta)) {
                continue;
            }

            for (OperacionHistorial operacion
                    : servicio.getOperaciones()) {

                int operacionId = operacion.getCatalogoId();

                UltimaOperacion anterior =
                        ultimas.get(operacionId);

                // Conservar la entrega mas reciente de cada operacion.
                if (anterior == null
                        || entrega.isAfter(anterior.fechaEntrega)) {

                    ultimas.put(
                            operacionId,
                            new UltimaOperacion(
                                    operacion,
                                    entrega
                            )
                    );
                }
            }
        }

        // Crear un recordatorio solo si la operacion
        // tiene un intervalo de mantenimiento definido.
        return ultimas.values().stream()
                .filter(ultima ->
                        ultima.operacion.getIntervaloMeses() != null
                )
                .map(ultima -> new RecordatorioPreventivo(
                        vehiculoId,
                        ultima.operacion.getCatalogoId(),
                        ultima.operacion.getNombre(),
                        ultima.fechaEntrega.toLocalDate(),
                        ultima.operacion.getIntervaloMeses(),
                        fechaConsulta
                ))
                .sorted(
                        Comparator.comparing(
                                RecordatorioPreventivo::getFechaProxima
                        ).thenComparingInt(
                                RecordatorioPreventivo::getOperacionId
                        )
                )
                .toList();
    }

    // CLASE AUXILIAR INTERNA
    // Conserva temporalmente la ultima operacion encontrada.
    private static class UltimaOperacion {

        private final OperacionHistorial operacion;
        private final LocalDateTime fechaEntrega;

        private UltimaOperacion(
                OperacionHistorial operacion,
                LocalDateTime fechaEntrega) {

            this.operacion = operacion;
            this.fechaEntrega = fechaEntrega;
        }
    }
}
