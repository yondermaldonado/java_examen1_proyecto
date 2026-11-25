package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.RepuestoTipo;
import com.carmotors.servicio.RepuestoTipoService;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * CONTROLADOR: recibe los textos escritos en MenuPruebasInventario.
 * Convierte los ID a números, llama a RepuestoTipoService y devuelve mensajes.
 * NO ejecuta SQL ni decide las reglas de negocio.
 */
public final class RepuestoTipoController {

    private final RepuestoTipoService servicio;

    public RepuestoTipoController(RepuestoTipoService servicio) {
        this.servicio = Objects.requireNonNull(servicio);
    }

    // Menú -> Controller -> Service.crear() -> DAO -> MySQL.
    public String crear(String nombre) {
        return ejecutar(() -> {
            int idCreado = servicio.crear(nombre);
            return "Tipo creado con ID " + idCreado + ".";
        });
    }

    // Convierte el ID de String a int y llama al servicio.
    public String actualizar(String id, String nombre) {
        return ejecutar(() -> {
            int idTipo = Integer.parseInt(id.trim());
            boolean actualizado = servicio.actualizar(idTipo, nombre);

            if (actualizado) {
                return "Tipo actualizado.";
            }
            return "No existe el tipo indicado.";
        });
    }

    // Pide al Service eliminar; el DAO comprueba el resultado de DELETE.
    public String eliminar(String id) {
        return ejecutar(() -> {
            int idTipo = Integer.parseInt(id.trim());
            boolean eliminado = servicio.eliminar(idTipo);

            if (eliminado) {
                return "Tipo eliminado.";
            }
            return "No existe el tipo indicado.";
        });
    }

    // Solicita la lista al Service y la convierte en texto para consola.
    public String listar() {
        return ejecutar(() -> {
            List<RepuestoTipo> tipos = servicio.listar();
            return formatear(tipos);
        });
    }

    // Optional permite representar que la consulta no encontró un tipo.
    public String buscarPorId(String id) {
        return ejecutar(() -> {
            int idTipo = Integer.parseInt(id.trim());
            Optional<RepuestoTipo> resultado = servicio.buscarPorId(idTipo);

            if (resultado.isEmpty()) {
                return "No se encontró el tipo.";
            }

            RepuestoTipo tipo = resultado.get();
            return tipo.getId() + " | " + tipo.getNombre();
        });
    }

    // Busca coincidencias y las muestra una por línea.
    public String buscarPorNombre(String nombre) {
        return ejecutar(() -> {
            List<RepuestoTipo> tipos = servicio.buscarPorNombre(nombre);
            return formatear(tipos);
        });
    }

    // Convierte objetos RepuestoTipo en un texto entendible para el menú.
    private String formatear(List<RepuestoTipo> tipos) {
        if (tipos.isEmpty()) {
            return "No se encontraron tipos de repuesto.";
        }

        StringBuilder texto = new StringBuilder();
        for (RepuestoTipo tipo : tipos) {
            texto.append(tipo.getId());
            texto.append(" | ");
            texto.append(tipo.getNombre());
            texto.append('\n');
        }
        return texto.toString();
    }

    // Ejecuta una lambda y convierte excepciones en mensajes para consola.
    // Centraliza los mensajes de error para evitar repetir try/catch.
    private String ejecutar(Supplier<String> operacion) {
        try {
            return operacion.get();
        } catch (NumberFormatException e) {
            return "Error: introduce un ID numérico válido.";
        } catch (ValidacionException e) {
            return "Error: " + e.getMessage();
        } catch (DaoException | ConexionException e) {
            return "Error del sistema: " + e.getMessage();
        }
    }
}
