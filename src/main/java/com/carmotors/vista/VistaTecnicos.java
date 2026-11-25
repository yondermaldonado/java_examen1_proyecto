package com.carmotors.vista;

import com.carmotors.controlador.ConsultaTallerController;
import com.carmotors.controlador.TecnicoController;
import com.carmotors.modelo.Tecnico;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Administración del personal técnico.
 * Usa controladores reales; no accede a JDBC ni modifica reglas de negocio.
 * La disponibilidad de un técnico se consulta en VistaSupervision, no mediante activo.
 */
public final class VistaTecnicos {
    private final Consola consola;
    private final TecnicoController operaciones;
    private final ConsultaTallerController consultas;

    public VistaTecnicos(Consola consola, TecnicoController operaciones,
                         ConsultaTallerController consultas) {
        this.consola = Objects.requireNonNull(consola, "La consola es obligatoria");
        this.operaciones = Objects.requireNonNull(operaciones, "El controlador de técnicos es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "El controlador de consultas es obligatorio");
    }

    /** Esta vista solo se ofrece desde MenuGestionAdministrativa. */
    public void mostrarGestion() {
        while (true) {
            consola.titulo("GESTIÓN DE TÉCNICOS");
            consola.mostrar("1. Listar técnicos");
            consola.mostrar("2. Buscar técnico por documento");
            consola.mostrar("3. Buscar técnico por ID");
            consola.mostrar("4. Registrar técnico");
            consola.mostrar("5. Actualizar técnico y estado activo/inactivo");
            consola.mostrar("6. Eliminar técnico sin órdenes relacionadas");
            consola.mostrar("0. Regresar");

            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "0");
            if (opcion == null || "0".equals(opcion)) return;

            switch (opcion) {
                case "1" -> ejecutar(this::listar);
                case "2" -> ejecutar(this::buscarDocumento);
                case "3" -> ejecutar(this::buscarId);
                case "4" -> ejecutar(this::registrar);
                case "5" -> ejecutar(this::actualizar);
                case "6" -> ejecutar(this::eliminar);
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }

    private void listar() {
        consola.titulo("TÉCNICOS REGISTRADOS");
        List<Tecnico> tecnicos = consultas.listarTecnicos();
        if (tecnicos.isEmpty()) {
            consola.mostrar("No hay técnicos registrados.");
        } else {
            tecnicos.forEach(this::mostrarTecnico);
        }
    }

    private void buscarDocumento() {
        consola.titulo("BUSCAR TÉCNICO POR DOCUMENTO");
        String documento = campoObligatorio("Documento de identidad");
        if (documento == null) return;
        // El Controller adapta y valida la consulta; no se interpreta el texto de respuesta.
        consola.mostrar(operaciones.buscarPorDocumento(documento));
    }

    private void buscarId() {
        consola.titulo("BUSCAR TÉCNICO POR ID");
        Integer id = leerId();
        if (id == null) return;
        consola.mostrar(operaciones.buscarPorId(id.toString()));
    }

    private void registrar() {
        consola.titulo("REGISTRAR TÉCNICO");
        consola.mostrar("Escribe 0 en cualquier campo para cancelar.");
        String nombre = campoObligatorio("Nombre completo");
        if (nombre == null) return;
        String documento = campoObligatorio("Documento de identidad");
        if (documento == null) return;
        String telefono = campoOpcional("Teléfono (ENTER = sin teléfono)");
        if (telefono == null) return;
        String especialidad = campoOpcional("Especialidad (ENTER = sin especialidad)");
        if (especialidad == null) return;

        consola.mostrar("Nombre: " + nombre + " | Documento: " + documento);
        consola.mostrar("Teléfono: " + mostrarOpcional(telefono) + " | Especialidad: "
                + mostrarOpcional(especialidad));
        if (!consola.confirmar("¿Registrar este técnico en MySQL?")) {
            consola.mostrar("Registro cancelado.");
            return;
        }
        // El ID del técnico nuevo lo genera MySQL, no la vista.
        consola.mostrar(operaciones.registrar(nombre, documento, telefono, especialidad));
    }

    private void actualizar() {
        consola.titulo("ACTUALIZAR TÉCNICO");
        Integer id = leerId();
        if (id == null) return;
        Optional<Tecnico> previo = buscarExistente(id);
        if (previo.isEmpty()) return;
        Tecnico original = previo.get();
        mostrarTecnico(original);
        consola.mostrar("ENTER conserva el valor; '-' borra los campos opcionales; 0 cancela.");

        String nombre = campoConDefecto("Nombre", original.getNombre(), false);
        if (nombre == null) return;
        String documento = campoConDefecto("Documento", original.getDocumentoIdentidad(), false);
        if (documento == null) return;
        String telefono = campoConDefecto("Teléfono", original.getTelefono(), true);
        if (telefono == null) return;
        String especialidad = campoConDefecto("Especialidad", original.getEspecialidad(), true);
        if (especialidad == null) return;
        Boolean activo = leerEstado(original.isActivo());
        if (activo == null) return;

        consola.mostrar("Nuevo registro: " + nombre + " | Documento: " + documento
                + " | Teléfono: " + mostrarOpcional(telefono)
                + " | Especialidad: " + mostrarOpcional(especialidad)
                + " | " + (activo ? "Activo" : "Inactivo"));
        if (!consola.confirmar("¿Confirmas la actualización del técnico #" + id + "?")) {
            consola.mostrar("Actualización cancelada.");
            return;
        }
        consola.mostrar(operaciones.actualizar(id.toString(), nombre, documento,
                telefono, especialidad, activo));
    }

    private void eliminar() {
        consola.titulo("ELIMINAR TÉCNICO");
        Integer id = leerId();
        if (id == null) return;
        Optional<Tecnico> previo = buscarExistente(id);
        if (previo.isEmpty()) return;
        mostrarTecnico(previo.get());
        consola.mostrar("Un técnico con órdenes relacionadas no puede eliminarse: desactívalo desde Actualizar.");
        if (!consola.confirmar("¿Confirmas ELIMINAR al técnico #" + id + " de MySQL?")) {
            consola.mostrar("Eliminación cancelada.");
            return;
        }
        consola.mostrar(operaciones.eliminar(id.toString()));
    }

    private Optional<Tecnico> buscarExistente(int id) {
        Optional<Tecnico> tecnico = consultas.listarTecnicos().stream()
                .filter(t -> t.getId() == id).findFirst();
        if (tecnico.isEmpty()) consola.mostrar("No existe un técnico con ID #" + id + ".");
        return tecnico;
    }

    private Integer leerId() {
        while (true) {
            String dato = consola.leerTexto("ID de técnico existente (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            try {
                int id = Integer.parseInt(dato);
                if (id > 0) return id;
            } catch (NumberFormatException ignored) {
                // La vista valida el formato de entrada, no las reglas del Service.
            }
            consola.mostrar("Introduce un ID entero positivo o 0 para cancelar.");
        }
    }

    private String campoObligatorio(String nombre) {
        while (true) {
            String dato = consola.leerTexto(nombre + " (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            if (!dato.isBlank()) return dato;
            consola.mostrar("Este dato es obligatorio.");
        }
    }

    private String campoOpcional(String nombre) {
        String dato = consola.leerTexto(nombre + " (0 = cancelar)");
        if (dato == null || "0".equals(dato)) return null;
        return dato;
    }

    private String campoConDefecto(String nombre, String anterior, boolean opcional) {
        String visual = anterior == null ? "sin dato" : anterior;
        while (true) {
            String dato = consola.leerTexto(nombre + " [" + visual + "] (0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            if (dato.isBlank()) return anterior == null ? "" : anterior;
            if (opcional && "-".equals(dato)) return "";
            if (!opcional && "-".equals(dato)) {
                consola.mostrar("Este dato es obligatorio.");
                continue;
            }
            return dato;
        }
    }

    private Boolean leerEstado(boolean anterior) {
        while (true) {
            String dato = consola.leerTexto("Estado [" + (anterior ? "Activo" : "Inactivo")
                    + "] (S = activo, N = inactivo, ENTER = mantener, 0 = cancelar)");
            if (dato == null || "0".equals(dato)) return null;
            if (dato.isBlank()) return anterior;
            if ("S".equalsIgnoreCase(dato)) return true;
            if ("N".equalsIgnoreCase(dato)) return false;
            consola.mostrar("Indica S, N, ENTER o 0.");
        }
    }

    private void mostrarTecnico(Tecnico tecnico) {
        consola.mostrar("#" + tecnico.getId() + " | " + tecnico.getNombre()
                + " | Documento: " + tecnico.getDocumentoIdentidad()
                + " | Teléfono: " + mostrarOpcional(tecnico.getTelefono())
                + " | Especialidad: " + mostrarOpcional(tecnico.getEspecialidad())
                + " | " + (tecnico.isActivo() ? "Activo" : "Inactivo"));
    }

    private String mostrarOpcional(String valor) {
        return valor == null || valor.isBlank() ? "No registrado" : valor;
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
