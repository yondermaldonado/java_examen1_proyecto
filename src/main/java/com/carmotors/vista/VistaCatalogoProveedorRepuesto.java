package com.carmotors.vista;

import com.carmotors.controlador.ProveedorController;
import com.carmotors.controlador.ProveedorRepuestoController;
import com.carmotors.controlador.RepuestoController;
import com.carmotors.modelo.Proveedor;
import com.carmotors.modelo.ProveedorRepuesto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Catálogo N:M de ofertas proveedor-repuesto; no modifica costos históricos de lotes. */
public final class VistaCatalogoProveedorRepuesto {
    private final Consola consola;
    private final ProveedorRepuestoController ofertas;
    private final ProveedorController proveedores;
    private final RepuestoController repuestos;

    public VistaCatalogoProveedorRepuesto(Consola consola,
                                          ProveedorRepuestoController ofertas,
                                          ProveedorController proveedores,
                                          RepuestoController repuestos) {
        this.consola = Objects.requireNonNull(consola);
        this.ofertas = Objects.requireNonNull(ofertas);
        this.proveedores = Objects.requireNonNull(proveedores);
        this.repuestos = Objects.requireNonNull(repuestos);
    }

    public void mostrarGestion() {
        while (true) {
            consola.titulo("CATÁLOGO PROVEEDOR - REPUESTO");
            consola.mostrar("1. Ofertas de un proveedor");
            consola.mostrar("2. Proveedores de un repuesto");
            consola.mostrar("3. Consultar oferta exacta");
            consola.mostrar("4. Asociar un repuesto a un proveedor");
            consola.mostrar("5. Actualizar costo ofertado");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> ejecutar(this::listarPorProveedor);
                case "2" -> ejecutar(this::listarPorRepuesto);
                case "3" -> ejecutar(this::buscarOferta);
                case "4" -> ejecutar(this::registrarOferta);
                case "5" -> ejecutar(this::actualizarCosto);
                default -> throw new IllegalStateException("Opción previamente validada");
            }
        }
    }

    private void listarPorProveedor() {
        mostrarProveedores();
        Integer id = leerId("ID del proveedor");
        if (id == null) return;
        imprimirOfertas(ofertas.listarPorProveedor(id));
    }

    private void listarPorRepuesto() {
        consola.mostrar(repuestos.listar());
        Integer id = leerId("ID del repuesto");
        if (id == null) return;
        imprimirOfertas(ofertas.listarPorRepuesto(id));
    }

    private void buscarOferta() {
        Integer proveedorId = elegirProveedor();
        if (proveedorId == null) return;
        Integer repuestoId = elegirRepuesto();
        if (repuestoId == null) return;
        Optional<ProveedorRepuesto> resultado = ofertas.buscarAsociacion(proveedorId, repuestoId);
        consola.mostrar(resultado.map(this::formatear).orElse("No hay oferta registrada para esa pareja."));
    }

    private void registrarOferta() {
        Integer proveedorId = elegirProveedor();
        if (proveedorId == null) return;
        Integer repuestoId = elegirRepuesto();
        if (repuestoId == null) return;
        String costo = leerCosto();
        if (costo == null) return;
        consola.mostrar("Se agregará una oferta para la pareja proveedor-repuesto seleccionada.");
        if (!consola.confirmar("¿Registrar esta oferta en MySQL?")) return;
        ProveedorRepuesto creada = ofertas.registrarAsociacion(proveedorId, repuestoId, costo);
        consola.mostrar("Oferta registrada: " + formatear(creada));
    }

    private void actualizarCosto() {
        Integer proveedorId = elegirProveedor();
        if (proveedorId == null) return;
        Integer repuestoId = elegirRepuesto();
        if (repuestoId == null) return;
        Optional<ProveedorRepuesto> actual = ofertas.buscarAsociacion(proveedorId, repuestoId);
        if (actual.isEmpty()) {
            consola.mostrar("La asociación no existe. Utiliza la opción 4 para registrarla.");
            return;
        }
        consola.mostrar("Costo actual: " + actual.get().getCosto().toPlainString());
        String nuevoCosto = leerCosto();
        if (nuevoCosto == null) return;
        if (!consola.confirmar("¿Actualizar el costo vigente de esta oferta?")) return;
        boolean cambiado = ofertas.actualizarCosto(proveedorId, repuestoId, nuevoCosto);
        consola.mostrar(cambiado ? "Costo ofertado actualizado." : "No se actualizó la oferta.");
    }

    private Integer elegirProveedor() {
        mostrarProveedores();
        return leerId("ID del proveedor");
    }

    private Integer elegirRepuesto() {
        consola.mostrar(repuestos.listar());
        return leerId("ID del repuesto");
    }

    private void mostrarProveedores() {
        List<Proveedor> lista = proveedores.listarProveedores();
        if (lista.isEmpty()) consola.mostrar("No hay proveedores registrados.");
        for (Proveedor proveedor : lista) {
            consola.mostrar("#" + proveedor.getId() + " | " + proveedor.getNombre());
        }
    }

    private void imprimirOfertas(List<ProveedorRepuesto> lista) {
        if (lista.isEmpty()) {
            consola.mostrar("No se encontraron ofertas.");
            return;
        }
        for (ProveedorRepuesto oferta : lista) consola.mostrar(formatear(oferta));
    }

    private String formatear(ProveedorRepuesto oferta) {
        return "Proveedor #" + oferta.getProveedorId() + " | Repuesto #"
                + oferta.getRepuestoId() + " | costo vigente: " + oferta.getCosto().toPlainString();
    }

    private Integer leerId(String etiqueta) {
        while (true) {
            String valor = consola.leerTexto(etiqueta + " (0 para cancelar)");
            if (valor == null || valor.equals("0")) return null;
            try {
                int id = Integer.parseInt(valor);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un ID entero positivo o 0 para cancelar.");
        }
    }

    private String leerCosto() {
        while (true) {
            String valor = consola.leerTexto("Costo ofertado (COP; ENTER cancela)");
            if (valor == null || valor.isBlank()) return null;
            try {
                BigDecimal costo = new BigDecimal(valor);
                if (costo.signum() >= 0 && costo.compareTo(new BigDecimal("99999999.99")) <= 0
                        && costo.stripTrailingZeros().scale() <= 2) return valor;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Costo inválido. Usa un número entre 0 y 99999999.99 con máximo 2 decimales.");
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
