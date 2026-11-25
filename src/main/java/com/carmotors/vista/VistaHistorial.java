package com.carmotors.vista;

import com.carmotors.controlador.ClienteController;
import com.carmotors.controlador.HistorialController;
import com.carmotors.controlador.VehiculoController;
import com.carmotors.modelo.Cliente;
import com.carmotors.modelo.OperacionHistorial;
import com.carmotors.modelo.RecordatorioPreventivo;
import com.carmotors.modelo.ServicioHistorial;
import com.carmotors.modelo.Vehiculo;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * Consultas reales de historial y recordatorios preventivos.
 * La vista no calcula intervalos, no abre conexiones JDBC y no modifica registros.
 */
public final class VistaHistorial {
    private static final DateTimeFormatter FECHA_HORA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Consola consola;
    private final HistorialController historial;
    private final ClienteController clientes;
    private final VehiculoController vehiculos;

    public VistaHistorial(Consola consola, HistorialController historial,
                          ClienteController clientes, VehiculoController vehiculos) {
        this.consola = Objects.requireNonNull(consola, "La consola es obligatoria");
        this.historial = Objects.requireNonNull(historial, "El controlador de historial es obligatorio");
        this.clientes = Objects.requireNonNull(clientes, "El controlador de clientes es obligatorio");
        this.vehiculos = Objects.requireNonNull(vehiculos, "El controlador de vehículos es obligatorio");
    }

    /** Menú de consulta compartido por ambos roles. */
    public void mostrar() {
        while (true) {
            consola.titulo("HISTORIAL Y MANTENIMIENTO PREVENTIVO");
            consola.mostrar("1. Historial de un vehículo (por placa)");
            consola.mostrar("2. Historial de vehículos de un cliente (por documento)");
            consola.mostrar("3. Recordatorios de un vehículo (por placa)");
            consola.mostrar("4. Recordatorios de un cliente (por documento)");
            consola.mostrar("0. Regresar");

            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> consultarHistorialVehiculo();
                case "2" -> consultarHistorialCliente();
                case "3" -> consultarRecordatoriosVehiculo();
                case "4" -> consultarRecordatoriosCliente();
                default -> throw new IllegalStateException("Opción previamente validada");
            }
        }
    }

    private void consultarHistorialVehiculo() {
        consola.titulo("HISTORIAL DE VEHÍCULO");
        String placa = textoObligatorio("Placa del vehículo");
        if (placa == null) return;
        ejecutar(() -> {
            Optional<Vehiculo> encontrado = vehiculos.buscarVehiculoPorPlaca(placa);
            if (encontrado.isEmpty()) {
                consola.mostrar("No existe un vehículo con la placa indicada.");
                return;
            }
            Vehiculo vehiculo = encontrado.get();
            mostrarServicios("Vehículo " + vehiculo.getPlaca(),
                    historial.consultarHistorialVehiculo(vehiculo.getId()));
        });
    }

    private void consultarHistorialCliente() {
        consola.titulo("HISTORIAL POR CLIENTE");
        String documento = textoObligatorio("Documento del cliente");
        if (documento == null) return;
        ejecutar(() -> {
            Optional<Cliente> encontrado = clientes.buscarClientePorDocumento(documento);
            if (encontrado.isEmpty()) {
                consola.mostrar("No existe un cliente con ese documento.");
                return;
            }
            Cliente cliente = encontrado.get();
            Map<Integer, List<ServicioHistorial>> registros =
                    historial.consultarHistorialCliente(cliente.getId());
            if (registros.isEmpty()) {
                consola.mostrar("El cliente no tiene vehículos registrados.");
                return;
            }
            List<Vehiculo> asociados = vehiculos.listarVehiculosPorCliente(cliente.getId());
            consola.titulo("HISTORIAL | " + cliente.getNombre());
            for (Vehiculo vehiculo : asociados) {
                List<ServicioHistorial> servicios = registros.getOrDefault(vehiculo.getId(), List.of());
                consola.mostrar("Vehículo " + vehiculo.getPlaca() + " | "
                        + servicios.size() + " servicio(s)");
                for (ServicioHistorial servicio : servicios) mostrarResumen(servicio);
            }
            consola.mostrar("Para revisar operaciones y precios de un servicio, "
                    + "usa la consulta por placa.");
        });
    }

    private void consultarRecordatoriosVehiculo() {
        consola.titulo("RECORDATORIOS POR VEHÍCULO");
        String placa = textoObligatorio("Placa del vehículo");
        if (placa == null) return;
        ejecutar(() -> {
            Optional<Vehiculo> encontrado = vehiculos.buscarVehiculoPorPlaca(placa);
            if (encontrado.isEmpty()) {
                consola.mostrar("No existe un vehículo con la placa indicada.");
                return;
            }
            Vehiculo vehiculo = encontrado.get();
            mostrarRecordatorios("Vehículo " + vehiculo.getPlaca(),
                    historial.consultarRecordatoriosVehiculo(vehiculo.getId()));
        });
    }

    private void consultarRecordatoriosCliente() {
        consola.titulo("RECORDATORIOS POR CLIENTE");
        String documento = textoObligatorio("Documento del cliente");
        if (documento == null) return;
        ejecutar(() -> {
            Optional<Cliente> encontrado = clientes.buscarClientePorDocumento(documento);
            if (encontrado.isEmpty()) {
                consola.mostrar("No existe un cliente con ese documento.");
                return;
            }
            Cliente cliente = encontrado.get();
            Map<Integer, List<RecordatorioPreventivo>> recordatorios =
                    historial.consultarRecordatoriosCliente(cliente.getId());
            if (recordatorios.isEmpty()) {
                consola.mostrar("El cliente no tiene vehículos registrados.");
                return;
            }
            List<Vehiculo> asociados = vehiculos.listarVehiculosPorCliente(cliente.getId());
            for (Vehiculo vehiculo : asociados) {
                mostrarRecordatorios("Vehículo " + vehiculo.getPlaca(),
                        recordatorios.getOrDefault(vehiculo.getId(), List.of()));
            }
        });
    }

    /** Las consultas del historial no requieren elegir IDs escritos por el usuario. */
    private void mostrarServicios(String titulo, List<ServicioHistorial> servicios) {
        consola.titulo("SERVICIOS | " + titulo);
        if (servicios.isEmpty()) {
            consola.mostrar("No se encontraron servicios para este vehículo.");
            return;
        }
        while (true) {
            for (int i = 0; i < servicios.size(); i++) {
                mostrarResumen(servicios.get(i), Integer.toString(i + 1) + ". ");
            }
            consola.mostrar("0. Regresar");
            String[] opciones = IntStream.rangeClosed(0, servicios.size())
                    .mapToObj(Integer::toString).toArray(String[]::new);
            String opcion = consola.leerOpcion("Abrir detalle", opciones);
            if (opcion == null || opcion.equals("0")) return;
            ServicioHistorial servicio = servicios.get(Integer.parseInt(opcion) - 1);
            mostrarDetalle(servicio);
            consola.esperar();
        }
    }

    private void mostrarResumen(ServicioHistorial servicio) {
        mostrarResumen(servicio, "   ");
    }

    private void mostrarResumen(ServicioHistorial servicio, String prefijo) {
        consola.mostrar(prefijo + "Orden #" + servicio.getServicioId()
                + " | " + servicio.getTipoMantenimiento()
                + " | " + servicio.getEstado()
                + " | recepción: " + fecha(servicio.getFechaRecepcion()));
    }

    private void mostrarDetalle(ServicioHistorial servicio) {
        consola.titulo("DETALLE HISTÓRICO | ORDEN #" + servicio.getServicioId());
        consola.mostrar("Estado: " + servicio.getEstado());
        consola.mostrar("Tipo de mantenimiento: " + servicio.getTipoMantenimiento());
        consola.mostrar("Recepción: " + fecha(servicio.getFechaRecepcion()));
        consola.mostrar("Entrega: " + fecha(servicio.getFechaEntrega()));
        consola.mostrar("Problema: " + (servicio.getDescripcionProblema() == null
                ? "No especificado" : servicio.getDescripcionProblema()));
        List<OperacionHistorial> operaciones = servicio.getOperaciones();
        if (operaciones.isEmpty()) {
            consola.mostrar("No hay operaciones del catálogo registradas en este historial.");
            return;
        }
        consola.mostrar("Operaciones realizadas:");
        for (OperacionHistorial operacion : operaciones) {
            consola.mostrar(" - " + operacion.getNombre()
                    + " | cobrado: " + operacion.getPrecioCobrado().toPlainString()
                    + " | periodicidad: " + (operacion.getIntervaloMeses() == null
                    ? "sin intervalo" : operacion.getIntervaloMeses() + " meses"));
        }
    }

    private void mostrarRecordatorios(String titulo, List<RecordatorioPreventivo> recordatorios) {
        consola.titulo("RECORDATORIOS | " + titulo);
        if (recordatorios.isEmpty()) {
            consola.mostrar("No hay recordatorios calculables con el historial disponible.");
            return;
        }
        for (RecordatorioPreventivo r : recordatorios) {
            consola.mostrar(" - " + r.getNombreOperacion()
                    + " | última entrega: " + r.getFechaUltimaEntrega()
                    + " | cada " + r.getIntervaloMeses() + " mes(es)"
                    + " | próxima fecha: " + r.getFechaProxima()
                    + " | " + r.getEstado());
        }
    }

    private String fecha(LocalDateTime valor) {
        return valor == null ? "Sin registrar" : valor.format(FECHA_HORA);
    }

    private String textoObligatorio(String indicacion) {
        while (true) {
            String texto = consola.leerTexto(indicacion + " (0 = cancelar)");
            if (texto == null || texto.equals("0")) return null;
            if (!texto.isBlank()) return texto;
            consola.mostrar("[!] Este dato es obligatorio.");
        }
    }

    private void ejecutar(Runnable operacion) {
        try {
            operacion.run();
        } catch (RuntimeException e) {
            consola.mostrar("[!] No se completó la consulta: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        consola.esperar();
    }
}
