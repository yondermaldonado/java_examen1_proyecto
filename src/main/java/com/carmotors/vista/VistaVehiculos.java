package com.carmotors.vista;

import com.carmotors.controlador.ClienteController;
import com.carmotors.controlador.VehiculoController;
import com.carmotors.modelo.Cliente;
import com.carmotors.modelo.Vehiculo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Interfaz de vehículos respaldada por los Controller y Service reales.
 * La vista nunca genera identificadores ni abre conexiones JDBC.
 */
public final class VistaVehiculos {
    private final Consola consola;
    private final VehiculoController vehiculos;
    private final ClienteController clientes;

    public VistaVehiculos(Consola consola, VehiculoController vehiculos, ClienteController clientes) {
        this.consola = Objects.requireNonNull(consola);
        this.vehiculos = Objects.requireNonNull(vehiculos);
        this.clientes = Objects.requireNonNull(clientes);
    }

    public void mostrarGestion() {
        while (true) {
            consola.titulo("GESTIÓN DE VEHÍCULOS | ADMINISTRADOR");
            consola.mostrar("1. Registrar vehículo");
            consola.mostrar("2. Buscar por placa");
            consola.mostrar("3. Buscar por ID");
            consola.mostrar("4. Listar todos");
            consola.mostrar("5. Listar vehículos de un cliente");
            consola.mostrar("6. Actualizar vehículo");
            consola.mostrar("7. Eliminar vehículo");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "7", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> registrar();
                case "2" -> buscarPlaca();
                case "3" -> buscarId();
                case "4" -> listar();
                case "5" -> listarPorCliente();
                case "6" -> actualizar();
                case "7" -> eliminar();
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }

    public void mostrarParaTecnico() {
        while (true) {
            consola.titulo("CONSULTAR VEHÍCULOS | TÉCNICO");
            consola.mostrar("1. Buscar por placa");
            consola.mostrar("2. Listar vehículos");
            consola.mostrar("3. Listar vehículos por cliente");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> buscarPlaca();
                case "2" -> listar();
                case "3" -> listarPorCliente();
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }

    private void registrar() {
        consola.titulo("REGISTRAR VEHÍCULO");
        String placa = obligatorio("Placa");
        if (placa == null) return;
        Integer clienteId = seleccionarCliente();
        if (clienteId == null) return;
        consola.mostrar("Utiliza el ID de un modelo existente en el catálogo modelos_vehiculo.");
        Integer modeloId = enteroPositivo("ID del modelo existente");
        if (modeloId == null) return;
        if (!consola.confirmar("¿Confirmar registro en MySQL? Placa: " + placa)) return;
        ejecutar(() -> {
            Vehiculo registrado = vehiculos.registrarVehiculo(placa, clienteId, modeloId);
            consola.mostrar("[OK] Vehículo registrado; ID generado por MySQL: " + registrado.getId());
            mostrarVehiculo(registrado);
        });
    }

    private void buscarPlaca() {
        String placa = obligatorio("Placa del vehículo");
        if (placa == null) return;
        ejecutar(() -> {
            Optional<Vehiculo> hallado = vehiculos.buscarVehiculoPorPlaca(placa);
            if (hallado.isPresent()) mostrarVehiculo(hallado.get());
            else consola.mostrar("No existe un vehículo registrado con esa placa.");
        });
    }

    private void buscarId() {
        Integer id = enteroPositivo("ID del vehículo existente");
        if (id == null) return;
        ejecutar(() -> mostrarVehiculo(vehiculos.obtenerVehiculoPorId(id)));
    }

    private void listar() {
        ejecutar(() -> {
            List<Vehiculo> lista = vehiculos.listarVehiculos();
            if (lista.isEmpty()) consola.mostrar("No hay vehículos registrados.");
            else lista.forEach(this::mostrarVehiculo);
        });
    }

    private void listarPorCliente() {
        Integer clienteId = seleccionarCliente();
        if (clienteId == null) return;
        ejecutar(() -> {
            List<Vehiculo> lista = vehiculos.listarVehiculosPorCliente(clienteId);
            if (lista.isEmpty()) consola.mostrar("Ese cliente no tiene vehículos registrados.");
            else lista.forEach(this::mostrarVehiculo);
        });
    }

    private void actualizar() {
        Integer id = enteroPositivo("ID del vehículo a modificar");
        if (id == null) return;
        ejecutar(() -> {
            Vehiculo anterior = vehiculos.obtenerVehiculoPorId(id);
            mostrarVehiculo(anterior);
            consola.mostrar("ENTER conserva el dato. 0 cancela.");
            String placa = textoConDefecto("Placa", anterior.getPlaca());
            if (placa == null) return;
            Integer clienteId = enteroConDefecto("ID de cliente propietario", anterior.getClienteId());
            if (clienteId == null) return;
            Integer modeloId = enteroConDefecto("ID de modelo", anterior.getModeloId());
            if (modeloId == null) return;
            if (!consola.confirmar("¿Confirmar modificación de vehículo #" + id + " en MySQL?")) return;
            Vehiculo actualizado = vehiculos.actualizarVehiculo(id, placa, clienteId, modeloId);
            consola.mostrar("[OK] Vehículo actualizado.");
            mostrarVehiculo(actualizado);
        });
    }

    private void eliminar() {
        Integer id = enteroPositivo("ID del vehículo a eliminar");
        if (id == null) return;
        ejecutar(() -> {
            Vehiculo anterior = vehiculos.obtenerVehiculoPorId(id);
            mostrarVehiculo(anterior);
            consola.mostrar("No se permitirá eliminar un vehículo con referencias que deban preservarse.");
            if (!consola.confirmar("¿CONFIRMAS ELIMINAR el vehículo #" + id + " de MySQL?")) return;
            vehiculos.eliminarVehiculo(id);
            consola.mostrar("[OK] Vehículo eliminado.");
        });
    }

    /** Busca por documento: un ID de cliente se consulta, nunca se inventa. */
    private Integer seleccionarCliente() {
        String documento = obligatorio("Documento del propietario registrado");
        if (documento == null) return null;
        try {
            Optional<Cliente> resultado = clientes.buscarClientePorDocumento(documento);
            if (resultado.isEmpty()) {
                consola.mostrar("[!] No existe el cliente. Regístralo primero en el módulo Clientes.");
                consola.esperar();
                return null;
            }
            Cliente cliente = resultado.get();
            consola.mostrar("Propietario: #" + cliente.getId() + " | " + cliente.getNombre());
            return cliente.getId();
        } catch (RuntimeException e) {
            error(e);
            consola.esperar();
            return null;
        }
    }

    private String obligatorio(String texto) {
        while (true) {
            String dato = consola.leerTexto(texto + " (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            if (!dato.isBlank()) return dato;
            consola.mostrar("[!] Este dato es obligatorio.");
        }
    }

    private Integer enteroPositivo(String texto) {
        while (true) {
            String dato = consola.leerTexto(texto + " (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            try {
                int valor = Integer.parseInt(dato);
                if (valor > 0) return valor;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("[!] Escribe un entero positivo o 0 para regresar.");
        }
    }

    private String textoConDefecto(String texto, String defecto) {
        String dato = consola.leerTexto(texto + " [" + defecto + "] (0 = cancelar)");
        if (dato == null || "0".equals(dato)) return null;
        return dato.isBlank() ? defecto : dato;
    }

    private Integer enteroConDefecto(String texto, int defecto) {
        while (true) {
            String dato = consola.leerTexto(texto + " [" + defecto + "] (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            if (dato.isBlank()) return defecto;
            try {
                int valor = Integer.parseInt(dato);
                if (valor > 0) return valor;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("[!] Escribe un ID entero positivo.");
        }
    }

    private void mostrarVehiculo(Vehiculo v) {
        consola.mostrar("Vehículo #" + v.getId() + " | Placa: " + v.getPlaca()
                + " | Cliente #" + v.getClienteId() + " | Modelo #" + v.getModeloId());
    }

    private void error(RuntimeException e) {
        consola.mostrar("[!] No se completó la operación: "
                + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }

    private void ejecutar(Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException e) { error(e); }
        consola.esperar();
    }
}
