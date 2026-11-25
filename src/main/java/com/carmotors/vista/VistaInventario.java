package com.carmotors.vista;

import com.carmotors.controlador.AlertasInventarioController;
import com.carmotors.controlador.LoteController;
import com.carmotors.controlador.OrdenServicioController;
import com.carmotors.controlador.ProveedorController;
import com.carmotors.controlador.RepuestoController;
import com.carmotors.controlador.ReservasOrdenController;
import com.carmotors.modelo.Proveedor;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Consulta de existencias, recepción de lotes y reservas vinculadas a órdenes.
 * Toda operación de persistencia se delega a los Controller existentes.
 * Las reservas solo están disponibles desde el menú administrativo.
 */
public final class VistaInventario {
    private final Consola consola;
    private final RepuestoController repuestos;
    private final LoteController lotes;
    private final ProveedorController proveedores;
    private final ReservasOrdenController reservas;
    private final OrdenServicioController ordenes;
    private final AlertasInventarioController alertas;

    public VistaInventario(Consola consola, RepuestoController repuestos,
                           LoteController lotes, ProveedorController proveedores,
                           ReservasOrdenController reservas, OrdenServicioController ordenes,
                           AlertasInventarioController alertas) {
        this.consola = Objects.requireNonNull(consola);
        this.repuestos = Objects.requireNonNull(repuestos);
        this.lotes = Objects.requireNonNull(lotes);
        this.proveedores = Objects.requireNonNull(proveedores);
        this.reservas = Objects.requireNonNull(reservas);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.alertas = Objects.requireNonNull(alertas);
    }

    public void mostrarParaTecnico() {
        while (true) {
            consola.titulo("INVENTARIO | TÉCNICO");
            consola.mostrar("1. Consultar catálogo de repuestos");
            consola.mostrar("2. Consultar stock de un repuesto");
            consola.mostrar("3. Registrar ingreso de lote");
            consola.mostrar("4. Consultar lote por código");
            consola.mostrar("5. Consultar lotes de un repuesto");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> ejecutar(() -> consola.mostrar(repuestos.listar()));
                case "2" -> ejecutar(this::stock);
                case "3" -> ejecutar(this::recibirLote);
                case "4" -> ejecutar(this::buscarLote);
                case "5" -> ejecutar(this::lotesPorRepuesto);
                default -> throw new IllegalStateException("Opción validada previamente");
            }
        }
    }

    public void mostrarAdministracion() {
        while (true) {
            consola.titulo("INVENTARIO Y ABASTECIMIENTO | ADMINISTRADOR");
            consola.mostrar("1. Consultar catálogo de repuestos");
            consola.mostrar("2. Consultar stock de un repuesto");
            consola.mostrar("3. Alertas de inventario");
            consola.mostrar("4. Registrar ingreso de lote");
            consola.mostrar("5. Buscar lote por código");
            consola.mostrar("6. Consultar lotes por repuesto");
            consola.mostrar("7. Consultar lotes por proveedor");
            consola.mostrar("8. Consultar plan FEFO (sin reservar)");
            consola.mostrar("9. Agregar repuesto a una orden (reserva ACTIVA)");
            consola.mostrar("10. Ajustar cantidad total reservada");
            consola.mostrar("11. Liberar reserva de la orden");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6",
                    "7", "8", "9", "10", "11", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> ejecutar(() -> consola.mostrar(repuestos.listar()));
                case "2" -> ejecutar(this::stock);
                case "3" -> ejecutar(() -> consola.mostrar(alertas.mostrarReporte()));
                case "4" -> ejecutar(this::recibirLote);
                case "5" -> ejecutar(this::buscarLote);
                case "6" -> ejecutar(this::lotesPorRepuesto);
                case "7" -> ejecutar(this::lotesPorProveedor);
                case "8" -> ejecutar(this::planFefo);
                case "9" -> ejecutar(this::reservar);
                case "10" -> ejecutar(this::ajustar);
                case "11" -> ejecutar(this::liberar);
                default -> throw new IllegalStateException("Opción validada previamente");
            }
        }
    }

    private void stock() {
        consola.mostrar(repuestos.listar());
        String repuestoId = id("ID del repuesto");
        if (repuestoId != null) consola.mostrar(lotes.consultarStock(repuestoId));
    }

    private void recibirLote() {
        consola.titulo("RECEPCIÓN DE LOTE EN ALMACÉN");
        mostrarProveedores();
        String proveedorId = id("ID del proveedor registrado");
        if (proveedorId == null) return;
        if (!proveedores.existeProveedor(Integer.parseInt(proveedorId))) {
            consola.mostrar("El proveedor no existe. No se registró el lote.");
            return;
        }
        String codigo = requerido("Código único del lote");
        if (codigo == null) return;
        String fecha = consola.leerTexto("Fecha de ingreso (AAAA-MM-DD, ENTER = hoy; 0 = cancelar)");
        if (fecha == null || "0".equals(fecha)) return;
        if (fecha.isBlank()) fecha = LocalDate.now().toString();
        consola.mostrar(repuestos.listar());
        String repuestoId = id("ID del repuesto recibido");
        if (repuestoId == null) return;
        String cantidad = id("Cantidad de unidades recibidas");
        if (cantidad == null) return;
        String costo = importe("Costo unitario real de adquisición (por ejemplo 25000.00)");
        if (costo == null) return;
        String caducidad = consola.leerTexto("Fecha de caducidad (AAAA-MM-DD o ENTER si no aplica; 0 = cancelar)");
        if (caducidad == null || "0".equals(caducidad)) return;
        consola.mostrar("Lote: " + codigo + " | Proveedor #" + proveedorId
                + " | Repuesto #" + repuestoId + " | Cantidad: " + cantidad
                + " | Costo unitario: " + costo);
        if (!consola.confirmar("¿Confirmar la recepción del lote en MySQL?")) return;
        // El ID de lote lo genera MySQL; no se solicita al usuario.
        consola.mostrar(lotes.registrarIngreso(codigo, proveedorId, fecha,
                repuestoId, cantidad, costo, caducidad));
    }

    private void buscarLote() {
        String codigo = requerido("Código del lote");
        if (codigo != null) consola.mostrar(lotes.buscarPorCodigo(codigo));
    }

    private void lotesPorRepuesto() {
        consola.mostrar(repuestos.listar());
        String id = id("ID del repuesto");
        if (id != null) consola.mostrar(lotes.listarPorRepuesto(id));
    }

    private void lotesPorProveedor() {
        mostrarProveedores();
        String id = id("ID del proveedor");
        if (id != null) consola.mostrar(lotes.listarPorProveedor(id));
    }

    private void planFefo() {
        consola.mostrar(repuestos.listar());
        String id = id("ID del repuesto");
        if (id == null) return;
        String cantidad = id("Unidades requeridas");
        if (cantidad != null) consola.mostrar(lotes.seleccionarFefo(id, cantidad));
    }

    private void reservar() {
        consola.titulo("AGREGAR REPUESTO A ORDEN");
        consola.mostrar(ordenes.listar());
        String orden = id("Número de orden existente");
        if (orden == null) return;
        consola.mostrar(repuestos.listar());
        String repuesto = id("ID del repuesto");
        if (repuesto == null) return;
        String cantidad = id("Cantidad requerida");
        if (cantidad == null) return;
        if (!consola.confirmar("¿Agregar la línea y reservar las unidades disponibles?")) return;
        consola.mostrar(reservas.reservar(orden, repuesto, cantidad));
    }

    private void ajustar() {
        consola.mostrar(ordenes.listar());
        String orden = id("Número de orden");
        if (orden == null) return;
        consola.mostrar(repuestos.listar());
        String repuesto = id("ID del repuesto ya reservado");
        if (repuesto == null) return;
        String cantidad = id("Nueva cantidad TOTAL (no adicional)");
        if (cantidad == null) return;
        if (!consola.confirmar("¿Ajustar la reserva?")) return;
        consola.mostrar(reservas.ajustar(orden, repuesto, cantidad));
    }

    private void liberar() {
        consola.mostrar(ordenes.listar());
        String orden = id("Número de orden");
        if (orden == null) return;
        String repuesto = id("ID del repuesto reservado");
        if (repuesto == null) return;
        consola.mostrar("La línea del repuesto permanecerá en la orden, pero la reserva se liberará.");
        if (!consola.confirmar("¿Liberar esta reserva?")) return;
        consola.mostrar(reservas.liberar(orden, repuesto));
    }

    private void mostrarProveedores() {
        for (Proveedor proveedor : proveedores.listarProveedores()) {
            consola.mostrar("#" + proveedor.getId() + " | " + proveedor.getNombre());
        }
    }

    private String id(String etiqueta) {
        while (true) {
            String valor = consola.leerTexto(etiqueta + " (0 = cancelar)");
            if (valor == null || "0".equals(valor)) return null;
            try {
                if (Integer.parseInt(valor) > 0) return valor;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un entero positivo.");
        }
    }

    /** Permite cero como valor legítimo; ENTER cancela la operación. */
    private String importe(String etiqueta) {
        while (true) {
            String valor = consola.leerTexto(etiqueta + " (ENTER = cancelar)");
            if (valor == null || valor.isBlank()) return null;
            try {
                if (new java.math.BigDecimal(valor).signum() >= 0) return valor;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un importe numérico no negativo, con punto decimal.");
        }
    }

    private String requerido(String etiqueta) {
        while (true) {
            String valor = consola.leerTexto(etiqueta + " (0 = cancelar)");
            if (valor == null || "0".equals(valor)) return null;
            if (!valor.isBlank()) return valor;
            consola.mostrar("El dato es obligatorio.");
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
