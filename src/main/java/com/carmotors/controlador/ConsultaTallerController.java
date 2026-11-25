package com.carmotors.controlador;

import com.carmotors.modelo.CargaTecnico;
import com.carmotors.modelo.OrdenServicio;
import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.modelo.Tecnico;
import com.carmotors.servicio.AsignacionTecnicoService;
import com.carmotors.servicio.OrdenServicioService;
import com.carmotors.servicio.TecnicoService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Consultas estructuradas para las vistas del taller.
 * No accede a JDBC, no aplica cambios de estado y no formatea texto de consola.
 * Reutiliza los servicios ya existentes y deja sus reglas de negocio intactas.
 */
public final class ConsultaTallerController {
    private final TecnicoService tecnicos;
    private final AsignacionTecnicoService asignaciones;
    private final OrdenServicioService ordenes;

    public ConsultaTallerController(TecnicoService tecnicos,
                                    AsignacionTecnicoService asignaciones,
                                    OrdenServicioService ordenes) {
        this.tecnicos = Objects.requireNonNull(tecnicos);
        this.asignaciones = Objects.requireNonNull(asignaciones);
        this.ordenes = Objects.requireNonNull(ordenes);
    }

    /** También permite al administrador consultar historial de técnicos desactivados. */
    public List<Tecnico> listarTecnicos() {
        return tecnicos.listar();
    }

    /** Selección de un técnico activo. */
    public List<Tecnico> listarTecnicosActivos() {
        return tecnicos.listar().stream().filter(Tecnico::isActivo).toList();
    }

    public List<CargaTecnico> listarCargaEnProceso() {
        return asignaciones.listarCargaEnProceso();
    }

    public List<CargaTecnico> listarSinOrdenesAbiertas() {
        return asignaciones.listarSinOrdenesAbiertas();
    }

    public List<ServicioTecnico> listarPorTecnico(int tecnicoId) {
        return asignaciones.listarPorTecnico(tecnicoId);
    }

    public List<ServicioTecnico> listarPorOrden(int ordenId) {
        return asignaciones.listarPorOrden(ordenId);
    }

    public Optional<OrdenServicio> buscarOrden(int ordenId) {
        return ordenes.buscarPorId(ordenId);
    }

    public List<OrdenServicio> listarOrdenes() {
        return ordenes.listar();
    }
}
