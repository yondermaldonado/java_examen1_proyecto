package com.carmotors.vista;

import com.carmotors.controlador.ProveedorController;
import com.carmotors.modelo.Proveedor;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** CRUD real de proveedores y acceso a ofertas y evaluaciones. */
public final class VistaProveedores {
    private final Consola consola;
    private final ProveedorController proveedores;
    private final VistaCatalogoProveedorRepuesto catalogo;
    private final VistaEvaluacionesProveedor evaluaciones;

    public VistaProveedores(Consola consola, ProveedorController proveedores,
                            VistaCatalogoProveedorRepuesto catalogo,
                            VistaEvaluacionesProveedor evaluaciones) {
        this.consola = Objects.requireNonNull(consola);
        this.proveedores = Objects.requireNonNull(proveedores);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.evaluaciones = Objects.requireNonNull(evaluaciones);
    }

    public void mostrarGestion() {
        while (true) {
            consola.titulo("GESTIÓN DE PROVEEDORES");
            consola.mostrar("1. Listar proveedores");
            consola.mostrar("2. Buscar proveedor por nombre");
            consola.mostrar("3. Buscar proveedor por ID");
            consola.mostrar("4. Registrar proveedor");
            consola.mostrar("5. Actualizar proveedor");
            consola.mostrar("6. Eliminar proveedor sin relaciones");
            consola.mostrar("7. Catálogo proveedor-repuesto y costos");
            consola.mostrar("8. Evaluaciones de calidad y puntualidad");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "7", "8", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> ejecutar(this::listar);
                case "2" -> ejecutar(this::buscarPorNombre);
                case "3" -> ejecutar(this::buscarPorId);
                case "4" -> ejecutar(this::registrar);
                case "5" -> ejecutar(this::actualizar);
                case "6" -> ejecutar(this::eliminar);
                case "7" -> catalogo.mostrarGestion();
                case "8" -> evaluaciones.mostrarGestion();
                default -> throw new IllegalStateException("Opción validada previamente");
            }
        }
    }

    private void listar() {
        imprimir(proveedores.listarProveedores());
    }

    private void buscarPorNombre() {
        String nombre = obligatorio("Nombre o parte del nombre");
        if (nombre == null) return;
        imprimir(proveedores.buscarProveedoresPorNombre(nombre));
    }

    private void buscarPorId() {
        Integer id = leerId("ID del proveedor");
        if (id == null) return;
        Optional<Proveedor> proveedor = proveedores.buscarProveedorPorId(id);
        consola.mostrar(proveedor.map(this::formatear).orElse("No existe ese proveedor."));
    }

    private void registrar() {
        String nombre = obligatorio("Nombre del proveedor");
        if (nombre == null) return;
        String contacto = consola.leerTexto("Contacto (ENTER si no aplica, 0 para cancelar)");
        if (contacto == null || contacto.equals("0")) return;
        String email = consola.leerTexto("Correo (ENTER si no aplica, 0 para cancelar)");
        if (email == null || email.equals("0")) return;
        if (!consola.confirmar("¿Registrar este proveedor en MySQL?")) return;
        Proveedor creado = proveedores.registrarProveedor(nombre, contacto, email);
        consola.mostrar("Proveedor creado con ID generado #" + creado.getId());
        consola.mostrar(formatear(creado));
    }

    private void actualizar() {
        Integer id = leerId("ID del proveedor que deseas actualizar");
        if (id == null) return;
        Optional<Proveedor> encontrado = proveedores.buscarProveedorPorId(id);
        if (encontrado.isEmpty()) {
            consola.mostrar("No existe ese proveedor.");
            return;
        }
        Proveedor actual = encontrado.get();
        consola.mostrar("Registro actual: " + formatear(actual));
        consola.mostrar("ENTER conserva el dato. Escribe - para quitar un contacto o correo.");
        String nombre = consola.leerTexto("Nuevo nombre (0 cancela)");
        if (nombre == null || nombre.equals("0")) return;
        String contacto = consola.leerTexto("Nuevo contacto (0 cancela)");
        if (contacto == null || contacto.equals("0")) return;
        String correo = consola.leerTexto("Nuevo correo (0 cancela)");
        if (correo == null || correo.equals("0")) return;
        String nuevoNombre = nombre.isBlank() ? actual.getNombre() : nombre;
        String nuevoContacto = contacto.isBlank() ? actual.getContacto() : contacto.equals("-") ? null : contacto;
        String nuevoCorreo = correo.isBlank() ? actual.getEmail() : correo.equals("-") ? null : correo;
        if (!consola.confirmar("¿Guardar los cambios en MySQL?")) return;
        boolean actualizado = proveedores.actualizarProveedor(id, nuevoNombre, nuevoContacto, nuevoCorreo);
        consola.mostrar(actualizado ? "Proveedor actualizado." : "No se actualizó el proveedor.");
    }

    private void eliminar() {
        Integer id = leerId("ID del proveedor que deseas eliminar");
        if (id == null) return;
        Optional<Proveedor> encontrado = proveedores.buscarProveedorPorId(id);
        if (encontrado.isEmpty()) {
            consola.mostrar("No existe ese proveedor.");
            return;
        }
        consola.mostrar(formatear(encontrado.get()));
        consola.mostrar("La eliminación será rechazada si tiene ofertas, lotes o evaluaciones asociados.");
        if (!consola.confirmar("¿Eliminar definitivamente al proveedor?")) return;
        boolean eliminado = proveedores.eliminarProveedor(id);
        consola.mostrar(eliminado ? "Proveedor eliminado." : "No se eliminó el proveedor.");
    }

    private void imprimir(List<Proveedor> lista) {
        if (lista.isEmpty()) {
            consola.mostrar("No se encontraron proveedores.");
            return;
        }
        for (Proveedor proveedor : lista) consola.mostrar(formatear(proveedor));
    }

    private String formatear(Proveedor p) {
        return "#" + p.getId() + " | " + p.getNombre() + " | contacto: "
                + (p.getContacto() == null ? "No registrado" : p.getContacto()) + " | email: "
                + (p.getEmail() == null ? "No registrado" : p.getEmail());
    }

    private String obligatorio(String mensaje) {
        while (true) {
            String valor = consola.leerTexto(mensaje + " (0 para cancelar)");
            if (valor == null || valor.equals("0")) return null;
            if (!valor.isBlank()) return valor;
            consola.mostrar("Este dato es obligatorio.");
        }
    }

    private Integer leerId(String mensaje) {
        while (true) {
            String valor = consola.leerTexto(mensaje + " (0 para cancelar)");
            if (valor == null || valor.equals("0")) return null;
            try {
                int id = Integer.parseInt(valor);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un ID entero positivo o 0 para cancelar.");
        }
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            consola.mostrar("No se completó la operación: " + (e.getMessage() == null
                    ? e.getClass().getSimpleName() : e.getMessage()));
        }
        consola.esperar();
    }
}
