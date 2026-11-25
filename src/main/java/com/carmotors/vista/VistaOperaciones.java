package com.carmotors.vista;

import com.carmotors.controlador.OperacionOrdenController;
import java.util.Objects;

/** Entrada y salida del registro de trabajos, sin reglas de precios ni SQL. */
public final class VistaOperaciones {
    private final Consola consola;
    private final OperacionOrdenController operaciones;
    public VistaOperaciones(Consola consola, OperacionOrdenController operaciones) {
        this.consola = Objects.requireNonNull(consola);
        this.operaciones = Objects.requireNonNull(operaciones);
    }

    public void mostrar(int ordenId) {
        String orden = Integer.toString(ordenId);
        while (true) {
            consola.titulo("OPERACIONES Y MANO DE OBRA | ORDEN #" + ordenId);
            consola.mostrar("1. Consultar catálogo de operaciones y precios base");
            consola.mostrar("2. Consultar operaciones y cobros de la orden");
            consola.mostrar("3. Registrar operación realizada y precio acordado");
            consola.mostrar("4. Actualizar precio de una operación registrada");
            consola.mostrar("5. Retirar una operación de la orden");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "0");
            if (opcion == null || "0".equals(opcion)) return;
            try {
                if ("1".equals(opcion)) consola.mostrar(operaciones.catalogo());
                else if ("2".equals(opcion)) consola.mostrar(operaciones.listar(orden));
                else {
                    consola.mostrar("Operaciones registradas:");
                    consola.mostrar(operaciones.listar(orden));
                    if ("3".equals(opcion)) consola.mostrar(operaciones.catalogo());
                    String catalogo = dato("ID de operación del catálogo");
                    if (catalogo == null) continue;
                    String precio = "5".equals(opcion) ? null : dato("Importe total acordado por esta operación (punto decimal)");
                    if (!"5".equals(opcion) && precio == null) continue;
                    if (!consola.confirmar("¿Confirmar el cambio en la orden #" + ordenId + "?")) continue;
                    switch (opcion) {
                        case "3" -> operaciones.registrar(orden, catalogo, precio);
                        case "4" -> operaciones.actualizar(orden, catalogo, precio);
                        case "5" -> operaciones.quitar(orden, catalogo);
                        default -> throw new IllegalStateException("Opción ya validada");
                    }
                    consola.mostrar("Operaciones actualizadas.");
                    consola.mostrar(operaciones.listar(orden));
                }
            } catch (NumberFormatException e) {
                consola.mostrar("Escribe identificadores enteros y precios numéricos con punto decimal.");
            } catch (RuntimeException e) {
                consola.mostrar("No se completó la operación: " + e.getMessage());
            }
            consola.esperar();
        }
    }

    private String dato(String titulo) {
        String texto = consola.leerTexto(titulo + " (ENTER = cancelar)");
        return texto == null || texto.isBlank() ? null : texto;
    }
}
