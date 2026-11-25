package com.carmotors.vista;

import com.carmotors.controlador.ConsultaTallerController;
import com.carmotors.controlador.OrdenServicioController;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.modelo.Tecnico;
import com.carmotors.servicio.TiempoTrabajo;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

/** Trabajos vinculados al técnico seleccionado, usando consultas reales del taller. */
public final class VistaTrabajosTecnico {
    private final Consola consola;
    private final ConsultaTallerController consulta;
    private final OrdenServicioController ordenes;
    private final VistaOperaciones operaciones;

    public VistaTrabajosTecnico(Consola consola, ConsultaTallerController consulta,
                                OrdenServicioController ordenes, VistaOperaciones operaciones) {
        this.consola = Objects.requireNonNull(consola);
        this.consulta = Objects.requireNonNull(consulta);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.operaciones = Objects.requireNonNull(operaciones);
    }

    /** Devuelve el contexto actualizado; 0 conserva la sesión anterior; EOF la termina. */
    public SesionConsola seleccionarTecnico(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.TECNICO) {
            throw new IllegalArgumentException("Se requiere rol técnico");
        }
        while (true) {
            try {
                List<Tecnico> activos = consulta.listarTecnicosActivos();
                consola.titulo("SELECCIÓN DE TÉCNICO");
                if (activos.isEmpty()) {
                    consola.mostrar("No hay técnicos activos registrados.");
                    consola.esperar();
                    return sesion;
                }
                for (int i = 0; i < activos.size(); i++) {
                    Tecnico t = activos.get(i);
                    consola.mostrar((i + 1) + ". " + t.getNombre() + " | "
                            + (t.getEspecialidad() == null ? "Sin especialidad" : t.getEspecialidad()));
                }
                consola.mostrar("0. Cancelar");
                String[] opciones = IntStream.rangeClosed(0, activos.size())
                        .mapToObj(Integer::toString).toArray(String[]::new);
                String eleccion = consola.leerOpcion("Seleccione el número", opciones);
                if (eleccion == null) return null;
                int posicion = Integer.parseInt(eleccion);
                if (posicion == 0) return sesion;
                Tecnico elegido = activos.get(posicion - 1);
                consola.mostrar("Técnico seleccionado: " + elegido.getNombre());
                return sesion.conTecnico(elegido.getId());
            } catch (RuntimeException e) {
                error(e);
                consola.esperar();
                return sesion;
            }
        }
    }

    public void mostrar(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.TECNICO) {
            throw new IllegalArgumentException("Se requiere rol técnico");
        }
        if (sesion.tecnicoId() == null) {
            consola.mostrar("Selecciona un técnico para consultar sus trabajos.");
            return;
        }
        while (true) {
            consola.titulo("TRABAJOS DEL TÉCNICO #" + sesion.tecnicoId());
            consola.mostrar("1. Órdenes activas");
            consola.mostrar("2. Historial de trabajos");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "0");
            if (opcion == null || "0".equals(opcion)) return;
            listarTrabajos(sesion.tecnicoId(), "1".equals(opcion));
        }
    }

    private void listarTrabajos(int tecnicoId, boolean activas) {
        try {
            List<ServicioTecnico> trabajos = consulta.listarPorTecnico(tecnicoId).stream()
                    .filter(t -> activas == esActiva(t.estadoOrden())).toList();
            consola.titulo(activas ? "ÓRDENES ACTIVAS" : "HISTORIAL DE TRABAJOS");
            if (trabajos.isEmpty()) {
                consola.mostrar("No hay trabajos en esta sección.");
                consola.esperar();
                return;
            }
            for (int i = 0; i < trabajos.size(); i++) {
                ServicioTecnico t = trabajos.get(i);
                consola.mostrar((i + 1) + ". Orden #" + t.servicioId() + " | "
                        + t.estadoOrden() + " | horas: " + TiempoTrabajo.formatear(t.horasTrabajadas()));
            }
            consola.mostrar("0. Regresar");
            String[] opciones = IntStream.rangeClosed(0, trabajos.size())
                    .mapToObj(Integer::toString).toArray(String[]::new);
            String seleccionado = consola.leerOpcion("Abrir número", opciones);
            if (seleccionado == null || "0".equals(seleccionado)) return;
            ServicioTecnico relacion = trabajos.get(Integer.parseInt(seleccionado) - 1);
            verTrabajo(tecnicoId, relacion.servicioId());
        } catch (RuntimeException e) {
            error(e);
            consola.esperar();
        }
    }

    private void verTrabajo(int tecnicoId, int ordenId) {
        while (true) {
            try {
                // Recargar desde MySQL en cada interacción: nunca confiar en una lista antigua.
                boolean asignado = consulta.listarPorTecnico(tecnicoId).stream()
                        .anyMatch(a -> a.servicioId() == ordenId);
                if (!asignado) {
                    consola.mostrar("La orden ya no está asignada al técnico seleccionado.");
                    consola.esperar();
                    return;
                }
                Optional<OrdenServicio> encontrada = consulta.buscarOrden(ordenId);
                if (encontrada.isEmpty()) {
                    consola.mostrar("La orden ya no existe.");
                    consola.esperar();
                    return;
                }
                OrdenServicio orden = encontrada.get();
                consola.titulo("ORDEN #" + orden.getId());
                consola.mostrar("Placa: " + orden.getPlaca() + " | Estado: " + orden.getEstado());
                consola.mostrar("Mantenimiento: " + orden.getTipoMantenimiento());
                consola.mostrar("Problema: " + orden.getDescripcionProblema());
                consola.mostrar("1. Ver técnicos asignados y horas");
                if ("En Proceso".equalsIgnoreCase(orden.getEstado())) {
                    consola.mostrar("2. Completar orden");
                }
                consola.mostrar("3. Operaciones y mano de obra");
                consola.mostrar("0. Regresar");
                String opcion = "En Proceso".equalsIgnoreCase(orden.getEstado())
                        ? consola.leerOpcion("Opción", "1", "2", "3", "0")
                        : consola.leerOpcion("Opción", "1", "3", "0");
                if (opcion == null || "0".equals(opcion)) return;
                if ("1".equals(opcion)) {
                    var asignaciones = consulta.listarPorOrden(ordenId);
                    for (ServicioTecnico asignacion : asignaciones) {
                        consola.mostrar("Técnico: " + asignacion.nombreTecnico()
                                + " | Horas: " + TiempoTrabajo.formatear(asignacion.horasTrabajadas()));
                    }
                    consola.esperar();
                } else if ("3".equals(opcion)) {
                    operaciones.mostrar(ordenId);
                } else {
                    completarOrden(tecnicoId, orden);
                }
            } catch (RuntimeException e) {
                error(e);
                consola.esperar();
                return;
            }
        }
    }

    private void completarOrden(int tecnicoId, OrdenServicio orden) {
        // El cambio de estado afecta toda la orden y procesa consumos de inventario.
        List<ServicioTecnico> asignados = consulta.listarPorOrden(orden.getId());
        if (asignados.stream().noneMatch(a -> a.tecnicoId() == tecnicoId)) {
            consola.mostrar("El técnico seleccionado no tiene esta orden asignada.");
            consola.esperar();
            return;
        }
        consola.mostrar("Esta acción completa toda la orden y procesa los consumos de repuestos.");
        if (asignados.size() > 1) {
            consola.mostrar("Hay " + asignados.size() + " técnicos asignados a esta orden.");
        }
        if (!consola.confirmar("¿Confirmar finalización de la orden #" + orden.getId() + "?")) return;
        consola.mostrar(ordenes.cambiarEstado(Integer.toString(orden.getId()), "Completado"));
        consola.esperar();
    }

    private boolean esActiva(String estado) {
        return "Pendiente".equalsIgnoreCase(estado) || "En Proceso".equalsIgnoreCase(estado);
    }

    private void error(RuntimeException e) {
        consola.mostrar("No se pudo realizar la consulta: "
                + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
    }
}
