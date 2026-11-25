package com.carmotors.vista;

import com.carmotors.controlador.ProveedorController;
import com.carmotors.controlador.RepuestoController;
import com.carmotors.controlador.RepuestoTipoController;
import com.carmotors.modelo.Proveedor;

import java.util.Objects;

/** Administración del catálogo de repuestos y sus tipos, a través de Controller. */
public final class VistaCatalogoRepuestos {
    private final Consola consola;
    private final RepuestoController repuestos;
    private final RepuestoTipoController tipos;
    private final ProveedorController proveedores;

    public VistaCatalogoRepuestos(Consola consola, RepuestoController repuestos,
                                  RepuestoTipoController tipos, ProveedorController proveedores) {
        this.consola = Objects.requireNonNull(consola);
        this.repuestos = Objects.requireNonNull(repuestos);
        this.tipos = Objects.requireNonNull(tipos);
        this.proveedores = Objects.requireNonNull(proveedores);
    }

    public void mostrarGestion() {
        while (true) {
            consola.titulo("GESTIÓN DEL CATÁLOGO DE REPUESTOS");
            consola.mostrar("1. Listar catálogo");
            consola.mostrar("2. Buscar repuesto por nombre");
            consola.mostrar("3. Crear repuesto");
            consola.mostrar("4. Actualizar repuesto y costo del proveedor indicado");
            consola.mostrar("5. Eliminar repuesto sin referencias");
            consola.mostrar("6. Gestionar tipos de repuesto");
            consola.mostrar("0. Regresar");
            String op = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "0");
            if (op == null || "0".equals(op)) return;
            switch (op) {
                case "1" -> ejecutar(() -> consola.mostrar(repuestos.listar()));
                case "2" -> ejecutar(this::buscar);
                case "3" -> ejecutar(this::crear);
                case "4" -> ejecutar(this::actualizar);
                case "5" -> ejecutar(this::eliminar);
                case "6" -> gestionarTipos();
                default -> throw new IllegalStateException("Opción validada previamente");
            }
        }
    }

    private void buscar() {
        String nombre = requerido("Nombre o parte del nombre");
        if (nombre != null) consola.mostrar(repuestos.buscarPorNombre(nombre));
    }

    private void crear() {
        consola.mostrar(tipos.listar());
        String tipo = id("ID del tipo de repuesto existente");
        if (tipo == null) return;
        String marca = id("ID de marca de repuesto existente");
        if (marca == null) return;
        listarProveedores();
        String proveedor = id("ID del proveedor ofertante");
        if (proveedor == null) return;
        String nombre = requerido("Nombre del repuesto");
        if (nombre == null) return;
        String costo = importe("Costo vigente ofertado por el proveedor");
        if (costo == null) return;
        String precio = importe("Precio de venta");
        if (precio == null) return;
        String minimo = enteroNoNegativo("Stock mínimo");
        if (minimo == null) return;
        if (!consola.confirmar("¿Crear repuesto y asociarlo al proveedor en MySQL?")) return;
        consola.mostrar(repuestos.crear(nombre, tipo, marca, proveedor, costo, precio, minimo));
    }

    private void actualizar() {
        consola.mostrar(repuestos.listar());
        String repuestoId = id("ID del repuesto que se actualizará");
        if (repuestoId == null) return;
        consola.mostrar("Registro actual por proveedor:");
        consola.mostrar(repuestos.buscarPorId(repuestoId));
        consola.mostrar(tipos.listar());
        String tipo = id("Nuevo ID de tipo");
        if (tipo == null) return;
        String marca = id("Nuevo ID de marca");
        if (marca == null) return;
        listarProveedores();
        String proveedor = id("ID de proveedor cuya oferta se modificará");
        if (proveedor == null) return;
        String nombre = requerido("Nombre actualizado");
        if (nombre == null) return;
        String costo = importe("Costo vigente ofertado por ese proveedor");
        if (costo == null) return;
        String precio = importe("Precio de venta actualizado");
        if (precio == null) return;
        String minimo = enteroNoNegativo("Stock mínimo actualizado");
        if (minimo == null) return;
        consola.mostrar("Esta operación actualiza el repuesto y el costo de la relación proveedor-repuesto.");
        if (!consola.confirmar("¿Guardar los cambios en MySQL?")) return;
        consola.mostrar(repuestos.actualizar(repuestoId, nombre, tipo, marca,
                proveedor, costo, precio, minimo));
    }

    private void eliminar() {
        consola.mostrar(repuestos.listar());
        String repuestoId = id("ID del repuesto a eliminar");
        if (repuestoId == null) return;
        consola.mostrar(repuestos.buscarPorId(repuestoId));
        if (!consola.confirmar("¿Eliminar definitivamente el repuesto (solo si no tiene referencias)?")) return;
        consola.mostrar(repuestos.eliminar(repuestoId));
    }

    private void gestionarTipos() {
        while (true) {
            consola.titulo("TIPOS DE REPUESTO");
            consola.mostrar("1. Listar");
            consola.mostrar("2. Crear");
            consola.mostrar("3. Actualizar");
            consola.mostrar("4. Eliminar");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> ejecutar(() -> consola.mostrar(tipos.listar()));
                case "2" -> ejecutar(() -> {
                    String nombre = requerido("Nombre del nuevo tipo");
                    if (nombre != null && consola.confirmar("¿Crear tipo de repuesto?")) {
                        consola.mostrar(tipos.crear(nombre));
                    }
                });
                case "3" -> ejecutar(() -> {
                    consola.mostrar(tipos.listar());
                    String id = id("ID del tipo");
                    if (id == null) return;
                    String nombre = requerido("Nombre actualizado");
                    if (nombre != null && consola.confirmar("¿Actualizar tipo de repuesto?")) {
                        consola.mostrar(tipos.actualizar(id, nombre));
                    }
                });
                case "4" -> ejecutar(() -> {
                    consola.mostrar(tipos.listar());
                    String id = id("ID del tipo a eliminar");
                    if (id != null && consola.confirmar("¿Eliminar tipo si no está utilizado?")) {
                        consola.mostrar(tipos.eliminar(id));
                    }
                });
                default -> throw new IllegalStateException("Opción validada previamente");
            }
        }
    }

    private void listarProveedores() {
        for (Proveedor p : proveedores.listarProveedores()) {
            consola.mostrar("#" + p.getId() + " | " + p.getNombre());
        }
    }

    private String id(String mensaje) {
        while (true) {
            String texto = consola.leerTexto(mensaje + " (0 = cancelar)");
            if (texto == null || "0".equals(texto)) return null;
            try {
                if (Integer.parseInt(texto) > 0) return texto;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un entero positivo.");
        }
    }

    private String enteroNoNegativo(String mensaje) {
        while (true) {
            String texto = consola.leerTexto(mensaje + " (ENTER = cancelar)");
            if (texto == null || texto.isBlank()) return null;
            try {
                if (Integer.parseInt(texto) >= 0) return texto;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un entero mayor o igual a cero.");
        }
    }

    /** El precio o costo puede ser cero; ENTER cancela. */
    private String importe(String mensaje) {
        while (true) {
            String texto = consola.leerTexto(mensaje + " (ENTER = cancelar)");
            if (texto == null || texto.isBlank()) return null;
            try {
                if (new java.math.BigDecimal(texto).signum() >= 0) return texto;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un importe numérico no negativo.");
        }
    }

    private String requerido(String mensaje) {
        while (true) {
            String texto = consola.leerTexto(mensaje + " (0 = cancelar)");
            if (texto == null || "0".equals(texto)) return null;
            if (!texto.isBlank()) return texto;
            consola.mostrar("Este campo es obligatorio.");
        }
    }

    private void ejecutar(Runnable accion) {
        try {
            accion.run();
        } catch (RuntimeException e) {
            consola.mostrar("No se completó la operación: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        consola.esperar();
    }
}
