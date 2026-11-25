package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Tecnico;
import com.carmotors.servicio.TecnicoService;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/** Adapta datos de la futura consola; no lee entrada ni contiene SQL. */
public class TecnicoController {
    private final TecnicoService servicio;

    public TecnicoController(TecnicoService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    public String registrar(String nombre, String documento, String telefono, String especialidad) {
        return ejecutar(() -> "Técnico registrado con id " + servicio.registrar(datos(nombre, documento, telefono, especialidad)) + ".");
    }

    public String listar() {
        return ejecutar(() -> {
            String filas = servicio.listar().stream().map(this::formatear).collect(Collectors.joining(System.lineSeparator()));
            return filas.isEmpty() ? "No hay técnicos registrados." : filas;
        });
    }

    public String buscarPorId(String id) {
        return ejecutar(() -> servicio.buscarPorId(entero(id)).map(this::formatear).orElse("No existe un técnico con ese id."));
    }

    public String buscarPorDocumento(String documento) {
        return ejecutar(() -> servicio.buscarPorDocumento(documento).map(this::formatear).orElse("No existe un técnico con ese documento."));
    }

    public String actualizar(String id, String nombre, String documento, String telefono,
                             String especialidad, boolean activo) {
        return ejecutar(() -> {
            Tecnico tecnico = datos(nombre, documento, telefono, especialidad);
            tecnico.setId(entero(id));
            tecnico.setActivo(activo);
            return servicio.actualizar(tecnico) ? "Técnico actualizado." : "No existe un técnico con ese id.";
        });
    }

    public String eliminar(String id) {
        return ejecutar(() -> servicio.eliminar(entero(id)) ? "Técnico eliminado." : "No existe un técnico con ese id.");
    }

    private Tecnico datos(String nombre, String documento, String telefono, String especialidad) {
        Tecnico tecnico = new Tecnico();
        tecnico.setNombre(nombre);
        tecnico.setDocumentoIdentidad(documento);
        tecnico.setTelefono(telefono);
        tecnico.setEspecialidad(especialidad);
        return tecnico;
    }

    private int entero(String id) {
        try { return Integer.parseInt(id == null ? "" : id.trim()); }
        catch (NumberFormatException e) { throw new ValidacionException("Escribe un id entero válido."); }
    }

    private String formatear(Tecnico t) {
        return t.getId() + " | " + t.getNombre() + " | " + t.getDocumentoIdentidad()
                + " | " + (t.getTelefono() == null ? "Sin teléfono" : t.getTelefono())
                + " | " + (t.getEspecialidad() == null ? "Sin especialidad" : t.getEspecialidad())
                + " | " + (t.isActivo() ? "Activo" : "Inactivo");
    }

    private String ejecutar(Supplier<String> accion) {
        try { return accion.get(); }
        catch (ValidacionException e) { return "Error: " + e.getMessage(); }
        catch (DaoException | ConexionException e) { return "Error del sistema: " + e.getMessage(); }
    }
}
