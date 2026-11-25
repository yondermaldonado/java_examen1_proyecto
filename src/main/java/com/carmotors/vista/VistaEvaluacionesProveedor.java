package com.carmotors.vista;

import com.carmotors.controlador.ConsultaLotesController;
import com.carmotors.controlador.EvaluacionProveedorController;
import com.carmotors.controlador.ProveedorController;
import com.carmotors.modelo.EvaluacionProveedor;
import com.carmotors.modelo.ExistenciaLote;
import com.carmotors.modelo.Proveedor;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

/** Evaluaciones ligadas a lotes reales del mismo proveedor. */
public final class VistaEvaluacionesProveedor {
    private final Consola consola;
    private final EvaluacionProveedorController evaluaciones;
    private final ProveedorController proveedores;
    private final ConsultaLotesController lotes;

    public VistaEvaluacionesProveedor(Consola consola,
                                      EvaluacionProveedorController evaluaciones,
                                      ProveedorController proveedores,
                                      ConsultaLotesController lotes) {
        this.consola = Objects.requireNonNull(consola);
        this.evaluaciones = Objects.requireNonNull(evaluaciones);
        this.proveedores = Objects.requireNonNull(proveedores);
        this.lotes = Objects.requireNonNull(lotes);
    }

    public void mostrarGestion() {
        while (true) {
            consola.titulo("EVALUACIONES DE PROVEEDORES");
            consola.mostrar("1. Registrar evaluación de lote recibido");
            consola.mostrar("2. Consultar evaluaciones de un proveedor");
            consola.mostrar("3. Consultar evaluación por ID");
            consola.mostrar("4. Promedios de calidad y puntualidad");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "0");
            if (opcion == null || opcion.equals("0")) return;
            switch (opcion) {
                case "1" -> ejecutar(this::registrar);
                case "2" -> ejecutar(this::listarPorProveedor);
                case "3" -> ejecutar(this::buscarPorId);
                case "4" -> ejecutar(this::verPromedios);
                default -> throw new IllegalStateException("Opción previamente validada");
            }
        }
    }

    private void registrar() {
        Integer proveedorId = seleccionarProveedor();
        if (proveedorId == null) return;
        Integer loteId = seleccionarLote(proveedorId);
        if (loteId == null) return;
        String calidad = consola.leerOpcion("Calidad recibida (1 a 5)", "1", "2", "3", "4", "5", "0");
        if (calidad == null || calidad.equals("0")) return;
        String puntualidad = consola.leerOpcion("Puntualidad de entrega (1 a 5)", "1", "2", "3", "4", "5", "0");
        if (puntualidad == null || puntualidad.equals("0")) return;
        String fecha = leerFecha();
        if (fecha == null) return;
        String observaciones = consola.leerTexto("Observaciones (ENTER si no aplica)");
        if (observaciones == null) return;
        consola.mostrar("Proveedor #" + proveedorId + " | lote #" + loteId + " | calidad "
                + calidad + "/5 | puntualidad " + puntualidad + "/5 | " + fecha);
        if (!consola.confirmar("¿Guardar la evaluación en MySQL?")) return;
        EvaluacionProveedor registrada = evaluaciones.registrarEvaluacion(
                proveedorId, loteId, calidad, puntualidad, fecha, observaciones);
        consola.mostrar("Evaluación guardada con ID generado #" + registrada.getId());
    }

    private void listarPorProveedor() {
        Integer proveedorId = seleccionarProveedor();
        if (proveedorId == null) return;
        List<EvaluacionProveedor> lista = evaluaciones.listarPorProveedor(proveedorId);
        if (lista.isEmpty()) {
            consola.mostrar("Este proveedor todavía no tiene evaluaciones.");
            return;
        }
        for (EvaluacionProveedor e : lista) consola.mostrar(formatear(e));
    }

    private void buscarPorId() {
        Integer id = leerId("ID de la evaluación");
        if (id == null) return;
        Optional<EvaluacionProveedor> resultado = evaluaciones.buscarEvaluacionPorId(id);
        consola.mostrar(resultado.map(this::formatear).orElse("No existe la evaluación."));
    }

    private void verPromedios() {
        Integer proveedorId = seleccionarProveedor();
        if (proveedorId == null) return;
        OptionalDouble calidad = evaluaciones.promedioCalidad(proveedorId);
        OptionalDouble puntualidad = evaluaciones.promedioPuntualidad(proveedorId);
        if (calidad.isEmpty() || puntualidad.isEmpty()) {
            consola.mostrar("Este proveedor todavía no tiene evaluaciones.");
            return;
        }
        consola.mostrar("Calidad promedio: " + String.format(Locale.ROOT, "%.2f", calidad.getAsDouble()) + "/5");
        consola.mostrar("Puntualidad promedio: "
                + String.format(Locale.ROOT, "%.2f", puntualidad.getAsDouble()) + "/5");
    }

    private Integer seleccionarProveedor() {
        List<Proveedor> lista = proveedores.listarProveedores();
        if (lista.isEmpty()) {
            consola.mostrar("No existen proveedores registrados.");
            return null;
        }
        for (Proveedor p : lista) consola.mostrar("#" + p.getId() + " | " + p.getNombre());
        Integer id = leerId("ID del proveedor");
        if (id == null) return null;
        boolean existe = lista.stream().anyMatch(p -> p.getId() == id);
        if (!existe) {
            consola.mostrar("El proveedor seleccionado no figura en el catálogo.");
            return null;
        }
        return id;
    }

    /** Elegir el número visual evita pedir al usuario un ID interno de lote. */
    private Integer seleccionarLote(int proveedorId) {
        Map<Integer, ExistenciaLote> unicos = new LinkedHashMap<>();
        for (ExistenciaLote fila : lotes.listarPorProveedor(proveedorId)) {
            unicos.putIfAbsent(fila.getLoteId(), fila);
        }
        if (unicos.isEmpty()) {
            consola.mostrar("El proveedor no tiene lotes registrados para evaluar.");
            return null;
        }
        List<ExistenciaLote> lista = new ArrayList<>(unicos.values());
        consola.mostrar("Lotes recibidos de este proveedor:");
        for (int i = 0; i < lista.size(); i++) {
            ExistenciaLote lote = lista.get(i);
            consola.mostrar((i + 1) + ". " + lote.getCodigo() + " | ingreso: " + lote.getFechaIngreso());
        }
        while (true) {
            String texto = consola.leerTexto("Número del lote (0 para cancelar)");
            if (texto == null || texto.equals("0")) return null;
            try {
                int numero = Integer.parseInt(texto);
                if (numero >= 1 && numero <= lista.size()) return lista.get(numero - 1).getLoteId();
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Elige un número de la lista o 0 para cancelar.");
        }
    }

    private String leerFecha() {
        while (true) {
            String texto = consola.leerTexto("Fecha de evaluación (AAAA-MM-DD, 0 cancela)");
            if (texto == null || texto.equals("0")) return null;
            try {
                return LocalDate.parse(texto).toString();
            } catch (DateTimeParseException e) {
                consola.mostrar("Escribe una fecha real con formato AAAA-MM-DD.");
            }
        }
    }

    private Integer leerId(String mensaje) {
        while (true) {
            String texto = consola.leerTexto(mensaje + " (0 para cancelar)");
            if (texto == null || texto.equals("0")) return null;
            try {
                int id = Integer.parseInt(texto);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) { }
            consola.mostrar("Introduce un entero positivo o 0 para cancelar.");
        }
    }

    private String formatear(EvaluacionProveedor e) {
        return "Evaluación #" + e.getId() + " | proveedor #" + e.getProveedorId()
                + " | lote #" + e.getLoteId()
                + " | calidad " + e.getCalificacionCalidad() + "/5"
                + " | puntualidad " + e.getCalificacionPuntualidad() + "/5"
                + " | fecha " + e.getFechaEvaluacion()
                + " | observaciones: " + (e.getObservaciones() == null ? "Sin observaciones" : e.getObservaciones());
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
