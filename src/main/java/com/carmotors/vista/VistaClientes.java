package com.carmotors.vista;

import com.carmotors.controlador.ClienteController;
import com.carmotors.modelo.Cliente;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Vista real de clientes: recoge entradas, delega en ClienteController y presenta resultados.
 * No abre conexiones, no conoce DAO ni asigna IDs de nuevas entidades.
 */
public final class VistaClientes {
    private final Consola consola;
    private final ClienteController controlador;

    public VistaClientes(Consola consola, ClienteController controlador) {
        this.consola = Objects.requireNonNull(consola, "La consola es obligatoria");
        this.controlador = Objects.requireNonNull(controlador, "El controlador de clientes es obligatorio");
    }

    /** Gestión completa reservada al administrador por la navegación. */
    public void mostrarGestion() {
        while (true) {
            consola.titulo("GESTIÓN DE CLIENTES");
            consola.mostrar("1. Registrar cliente");
            consola.mostrar("2. Listar clientes");
            consola.mostrar("3. Buscar por documento");
            consola.mostrar("4. Buscar por ID");
            consola.mostrar("5. Actualizar cliente");
            consola.mostrar("6. Eliminar cliente");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> registrar();
                case "2" -> listar();
                case "3" -> buscarDocumento();
                case "4" -> buscarId();
                case "5" -> actualizar();
                case "6" -> eliminar();
                default -> throw new IllegalStateException("Opción previamente validada");
            }
        }
    }

    /** Acceso operativo para técnicos: consultar y registrar, no actualizar ni eliminar. */
    public void mostrarParaTecnico() {
        while (true) {
            consola.titulo("CLIENTES | PERSONAL TÉCNICO");
            consola.mostrar("1. Registrar cliente");
            consola.mostrar("2. Buscar cliente por documento");
            consola.mostrar("3. Listar clientes");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> registrar();
                case "2" -> buscarDocumento();
                case "3" -> listar();
                default -> throw new IllegalStateException("Opción previamente validada");
            }
        }
    }

    /** Alta real: los ID los genera MySQL a través del DAO y el Service. */
    public void registrar() {
        consola.titulo("REGISTRAR CLIENTE");
        consola.mostrar("Escribe 0 en cualquier campo para cancelar el formulario.");
        String nombre = campoObligatorio("Nombre completo");
        if (nombre == null) return;
        String documento = campoObligatorio("Documento de identidad");
        if (documento == null) return;
        String telefono = campoObligatorio("Teléfono");
        if (telefono == null) return;
        String email = campoOpcional("Correo electrónico (ENTER = sin correo)");
        if (email == null) return;

        consola.mostrar("Cliente: " + nombre + " | Documento: " + documento);
        if (!consola.confirmar("¿Confirmas que deseas registrar este cliente en MySQL?")) {
            consola.mostrar("Registro cancelado; no se modificó la base de datos.");
            consola.esperar();
            return;
        }
        ejecutar(() -> {
            Cliente cliente = controlador.registrarCliente(nombre, documento, telefono, email);
            consola.mostrar("[OK] Cliente registrado. ID generado por MySQL: " + cliente.getId());
            mostrarCliente(cliente);
        });
    }

    private void listar() {
        consola.titulo("LISTADO DE CLIENTES");
        ejecutar(() -> {
            List<Cliente> clientes = controlador.listarClientes();
            if (clientes.isEmpty()) consola.mostrar("No hay clientes registrados.");
            else clientes.forEach(this::mostrarCliente);
        });
    }

    private void buscarDocumento() {
        consola.titulo("BUSCAR CLIENTE POR DOCUMENTO");
        String documento = campoObligatorio("Documento");
        if (documento == null) return;
        ejecutar(() -> {
            Optional<Cliente> cliente = controlador.buscarClientePorDocumento(documento);
            if (cliente.isPresent()) mostrarCliente(cliente.get());
            else consola.mostrar("No se encontró un cliente con ese documento.");
        });
    }

    private void buscarId() {
        consola.titulo("BUSCAR CLIENTE POR ID");
        Integer id = leerId();
        if (id == null) return;
        ejecutar(() -> mostrarCliente(controlador.obtenerClientePorId(id)));
    }

    private void actualizar() {
        consola.titulo("ACTUALIZAR CLIENTE");
        Integer id = leerId();
        if (id == null) return;
        ejecutar(() -> {
            Cliente previo = controlador.obtenerClientePorId(id);
            mostrarCliente(previo);
            consola.mostrar("ENTER conserva el valor; en correo, '-' elimina el correo. 0 cancela.");
            String nombre = campoConDefecto("Nombre", previo.getNombre());
            if (nombre == null) return;
            String documento = campoConDefecto("Documento", previo.getDocumento());
            if (documento == null) return;
            String telefono = campoConDefecto("Teléfono", previo.getTelefono());
            if (telefono == null) return;
            String emailActual = previo.getEmail() == null ? "" : previo.getEmail();
            String email = campoConDefecto("Correo", emailActual);
            if (email == null) return;
            if (email.equals("-")) email = null;
            if (!consola.confirmar("¿Confirmas actualizar este cliente en MySQL?")) {
                consola.mostrar("Actualización cancelada.");
                return;
            }
            Cliente actualizado = controlador.actualizarCliente(id, nombre, documento, telefono, email);
            consola.mostrar("[OK] Cliente actualizado.");
            mostrarCliente(actualizado);
        });
    }

    private void eliminar() {
        consola.titulo("ELIMINAR CLIENTE");
        Integer id = leerId();
        if (id == null) return;
        ejecutar(() -> {
            Cliente cliente = controlador.obtenerClientePorId(id);
            mostrarCliente(cliente);
            consola.mostrar("AVISO: los clientes con vehículos relacionados no pueden eliminarse.");
            if (!consola.confirmar("¿Confirmas ELIMINAR este cliente de MySQL?")) {
                consola.mostrar("Eliminación cancelada.");
                return;
            }
            controlador.eliminarCliente(id);
            consola.mostrar("[OK] Cliente eliminado.");
        });
    }

    private String campoObligatorio(String titulo) {
        while (true) {
            String dato = consola.leerTexto(titulo + " (0 = cancelar)");
            if (dato == null || dato.equals("0")) return null;
            if (!dato.isBlank()) return dato;
            consola.mostrar("[!] El campo es obligatorio.");
        }
    }

    private String campoOpcional(String titulo) {
        String dato = consola.leerTexto(titulo + " (0 = cancelar)");
        if (dato == null || dato.equals("0")) return null;
        return dato.isBlank() ? "" : dato;
    }

    private String campoConDefecto(String titulo, String defecto) {
        String dato = consola.leerTexto(titulo + " [" + defecto + "] (0 = cancelar)");
        if (dato == null || dato.equals("0")) return null;
        return dato.isBlank() ? defecto : dato;
    }

    private Integer leerId() {
        while (true) {
            String dato = consola.leerTexto("ID de cliente existente (0 = regresar)");
            if (dato == null || dato.equals("0")) return null;
            try {
                int id = Integer.parseInt(dato);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // La vista solo valida la entrada; los errores de negocio corresponden al Service.
            }
            consola.mostrar("[!] Introduce un ID entero positivo, o 0 para cancelar.");
        }
    }

    private void mostrarCliente(Cliente cliente) {
        consola.mostrar("#" + cliente.getId() + " | " + cliente.getNombre()
                + " | Documento: " + cliente.getDocumento()
                + " | Teléfono: " + cliente.getTelefono()
                + " | Correo: " + (cliente.getEmail() == null ? "Sin correo" : cliente.getEmail()));
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (IllegalArgumentException | IllegalStateException e) {
            consola.mostrar("[!] Validación: " + e.getMessage());
        } catch (RuntimeException e) {
            // Presentación: no ocultamos el motivo al operador, pero dejamos la causa original en la excepción.
            consola.mostrar("[!] No se completó la operación: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        consola.esperar();
    }
}
