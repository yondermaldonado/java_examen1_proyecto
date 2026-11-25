package com.carmotors.vista;

import com.carmotors.controlador.AsignacionTecnicoController;
import com.carmotors.controlador.ConsultaTallerController;
import com.carmotors.modelo.CargaTecnico;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.modelo.Tecnico;
import com.carmotors.servicio.TiempoTrabajo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/** Vista administrativa de las asignaciones N:M y el tiempo acumulado. */
public final class VistaSupervision {
    private final Consola consola;
    private final ConsultaTallerController consulta;
    private final AsignacionTecnicoController asignaciones;

    public VistaSupervision(Consola consola, ConsultaTallerController consulta,
                            AsignacionTecnicoController asignaciones) {
        this.consola = Objects.requireNonNull(consola);
        this.consulta = Objects.requireNonNull(consulta);
        this.asignaciones = Objects.requireNonNull(asignaciones);
    }

    public void mostrar(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.ADMINISTRADOR) {
            throw new IllegalArgumentException("Se requiere rol administrativo");
        }
        while (true) {
            consola.titulo("SUPERVISIÓN DEL TALLER");
            consola.mostrar("1. Técnicos con órdenes en proceso");
            consola.mostrar("2. Técnicos sin órdenes abiertas");
            consola.mostrar("3. Consultar trabajos de un técnico");
            consola.mostrar("4. Consultar técnicos de una orden");
            consola.mostrar("5. Asignar técnico a una orden");
            consola.mostrar("6. Registrar tiempo trabajado (acumulativo HH:mm)");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> ejecutar(this::listarOcupados);
                case "2" -> ejecutar(this::listarSinOrdenes);
                case "3" -> ejecutar(this::consultarPorTecnico);
                case "4" -> ejecutar(this::consultarPorOrden);
                case "5" -> ejecutar(this::asignar);
                case "6" -> ejecutar(this::registrarTiempo);
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }

    private void listarOcupados() {
        List<CargaTecnico> filas = consulta.listarCargaEnProceso();
        if (filas.isEmpty()) {
            consola.mostrar("No hay técnicos con órdenes en proceso.");
            return;
        }
        for (CargaTecnico t : filas) {
            consola.mostrar("#" + t.tecnicoId() + " | " + t.nombre() + " | "
                    + t.ordenesEnProceso() + " orden(es) | " + (t.activo() ? "Activo" : "Inactivo"));
        }
    }

    private void listarSinOrdenes() {
        List<CargaTecnico> filas = consulta.listarSinOrdenesAbiertas();
        if (filas.isEmpty()) {
            consola.mostrar("Todos los técnicos registrados tienen órdenes abiertas.");
            return;
        }
        for (CargaTecnico t : filas) {
            consola.mostrar("#" + t.tecnicoId() + " | " + t.nombre()
                    + " | Sin órdenes abiertas | " + (t.activo() ? "Activo" : "Inactivo"));
        }
    }

    private void consultarPorTecnico() {
        Integer id = elegirTecnico(false);
        if (id == null) return;
        List<ServicioTecnico> filas = consulta.listarPorTecnico(id);
        if (filas.isEmpty()) {
            consola.mostrar("Este técnico no tiene trabajos registrados.");
            return;
        }
        for (ServicioTecnico asignacion : filas) {
            consola.mostrar("Orden #" + asignacion.servicioId() + " | " + asignacion.estadoOrden()
                    + " | Horas: " + TiempoTrabajo.formatear(asignacion.horasTrabajadas()));
        }
    }

    private void consultarPorOrden() {
        Integer id = elegirOrden();
        if (id == null) return;
        List<ServicioTecnico> filas = consulta.listarPorOrden(id);
        if (filas.isEmpty()) {
            consola.mostrar("La orden no tiene técnicos asignados.");
            return;
        }
        for (ServicioTecnico asignacion : filas) {
            consola.mostrar("Técnico #" + asignacion.tecnicoId() + " | " + asignacion.nombreTecnico()
                    + " | Horas: " + TiempoTrabajo.formatear(asignacion.horasTrabajadas()));
        }
    }

    private void asignar() {
        Integer ordenId = elegirOrden();
        if (ordenId == null) return;
        Optional<OrdenServicio> orden = consulta.buscarOrden(ordenId);
        if (orden.isEmpty()) {
            consola.mostrar("No se encontró la orden.");
            return;
        }
        if (!List.of("Pendiente", "En Proceso").contains(orden.get().getEstado())) {
            consola.mostrar("Solo se pueden asignar técnicos a órdenes pendientes o en proceso.");
            return;
        }
        Integer tecnicoId = elegirTecnico(true);
        if (tecnicoId == null) return;
        if (consulta.listarPorOrden(ordenId).stream().anyMatch(t -> t.tecnicoId() == tecnicoId)) {
            consola.mostrar("El técnico ya tiene asignada esa orden.");
            return;
        }
        consola.mostrar("Asignar técnico #" + tecnicoId + " a orden #" + ordenId + ".");
        if ("Pendiente".equals(orden.get().getEstado())) {
            consola.mostrar("La asignación también inicia la orden automáticamente.");
        }
        if (!consola.confirmar("¿Confirmar asignación?")) return;
        consola.mostrar(asignaciones.asignar(ordenId.toString(), tecnicoId.toString()));
    }

    private void registrarTiempo() {
        Integer ordenId = elegirOrden();
        if (ordenId == null) return;
        List<ServicioTecnico> trabajadores = consulta.listarPorOrden(ordenId);
        if (trabajadores.isEmpty()) {
            consola.mostrar("No hay técnicos asignados a esta orden.");
            return;
        }
        for (int i = 0; i < trabajadores.size(); i++) {
            ServicioTecnico t = trabajadores.get(i);
            consola.mostrar((i + 1) + ". " + t.nombreTecnico()
                    + " | Acumulado: " + TiempoTrabajo.formatear(t.horasTrabajadas()));
        }
        consola.mostrar("0. Cancelar");
        String[] opciones = IntStream.rangeClosed(0, trabajadores.size())
                .mapToObj(Integer::toString).toArray(String[]::new);
        String posicion = consola.leerOpcion("Selecciona un técnico", opciones);
        if (posicion == null || "0".equals(posicion)) return;
        ServicioTecnico seleccionado = trabajadores.get(Integer.parseInt(posicion) - 1);
        String tiempo = consola.leerTexto("Tiempo realizado (HH:mm; por ejemplo 02:30, 0 = cancelar)");
        if (tiempo == null || "0".equals(tiempo)) return;
        if (!consola.confirmar("¿Sumar " + tiempo + " a las horas de " + seleccionado.nombreTecnico() + "?")) return;
        String respuesta = asignaciones.registrarHoras(ordenId.toString(),
                Integer.toString(seleccionado.tecnicoId()), tiempo);
        consola.mostrar(respuesta);
        if (!respuesta.startsWith("Error")) {
            consulta.listarPorOrden(ordenId).stream()
                    .filter(t -> t.tecnicoId() == seleccionado.tecnicoId())
                    .findFirst()
                    .ifPresent(t -> consola.mostrar("Nuevo acumulado: "
                            + TiempoTrabajo.formatear(t.horasTrabajadas())));
        }
    }

    private Integer elegirTecnico(boolean soloActivos) {
        List<Tecnico> activos = soloActivos ? consulta.listarTecnicosActivos() : consulta.listarTecnicos();
        if (activos.isEmpty()) {
            consola.mostrar("No hay técnicos activos.");
            return null;
        }
        for (int i = 0; i < activos.size(); i++) {
            consola.mostrar((i + 1) + ". " + activos.get(i).getNombre()
                    + " (#" + activos.get(i).getId() + ") | "
                    + (activos.get(i).isActivo() ? "Activo" : "Inactivo"));
        }
        consola.mostrar("0. Cancelar");
        String[] opciones = IntStream.rangeClosed(0, activos.size())
                .mapToObj(Integer::toString).toArray(String[]::new);
        String posicion = consola.leerOpcion("Selecciona un técnico", opciones);
        return posicion == null || "0".equals(posicion)
                ? null : activos.get(Integer.parseInt(posicion) - 1).getId();
    }

    private Integer elegirOrden() {
        List<OrdenServicio> ordenes = consulta.listarOrdenes();
        if (ordenes.isEmpty()) {
            consola.mostrar("No existen órdenes registradas.");
            return null;
        }
        for (int i = 0; i < ordenes.size(); i++) {
            OrdenServicio o = ordenes.get(i);
            consola.mostrar((i + 1) + ". Orden #" + o.getId() + " | " + o.getPlaca()
                    + " | " + o.getEstado());
        }
        consola.mostrar("0. Cancelar");
        String[] opciones = IntStream.rangeClosed(0, ordenes.size())
                .mapToObj(Integer::toString).toArray(String[]::new);
        String posicion = consola.leerOpcion("Selecciona la orden", opciones);
        return posicion == null || "0".equals(posicion)
                ? null : ordenes.get(Integer.parseInt(posicion) - 1).getId();
    }

    private void ejecutar(Runnable accion) {
        try { accion.run(); }
        catch (RuntimeException e) {
            consola.mostrar("No se completó la operación: "
                    + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
        }
        consola.esperar();
    }
}
