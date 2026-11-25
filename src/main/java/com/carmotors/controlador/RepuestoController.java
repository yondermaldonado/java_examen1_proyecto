package com.carmotors.controlador;

import com.carmotors.excepcion.ConexionException;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.Repuesto;
import com.carmotors.servicio.RepuestoService;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Supplier;

/** Recibe texto (lo que escribe el usuario), lo convierte y llama al servicio. Devuelve mensajes listos para mostrar. */
// CONTROLLER: recibe datos de texto desde la vista, los convierte a Java
// y llama a RepuestoService. No contiene consultas SQL.
// Las lambdas envían acciones al método ejecutar(), que controla los errores.
public class RepuestoController {
    private final RepuestoService servicio;

    public RepuestoController(RepuestoService servicio) {
        this.servicio = servicio;
    }

    // Recibe cadenas desde MenuPruebasInventario.
    // Construye Repuesto y llama a servicio.crear() dentro de ejecutar().
    public String crear(String nombre, String tipoId, String marcaId, String proveedorId,
                        String precioCompra, String precioVenta, String stockMinimo) {
        return ejecutar(() -> {
            Repuesto r = construir(0, nombre, tipoId, marcaId, proveedorId, precioCompra, precioVenta, stockMinimo);
            int id = servicio.crear(r);
            return "Repuesto creado con id " + id + ".";
        });
    }

    // Convierte los datos del formulario y pide actualizar al Service.
    public String actualizar(String id, String nombre, String tipoId, String marcaId, String proveedorId,
                             String precioCompra, String precioVenta, String stockMinimo) {
        return ejecutar(() -> {
            Repuesto r = construir(entero(id), nombre, tipoId, marcaId, proveedorId, precioCompra, precioVenta, stockMinimo);
            return servicio.actualizar(r) ? "Repuesto actualizado." : "No existe un repuesto con ese id.";
        });
    }

    // Convierte el ID a entero y solicita la eliminación al Service.
    public String eliminar(String id) {
        return ejecutar(() -> servicio.eliminar(entero(id)) ? "Repuesto eliminado." : "No existe un repuesto con ese id.");
    }

    public String listar() {
        return ejecutar(() -> formatear(servicio.listar()));
    }

    public String buscarPorId(String id) {
        return ejecutar(() -> formatear(servicio.buscarPorId(entero(id))));
    }

    public String buscarPorNombre(String texto) {
        return ejecutar(() -> formatear(servicio.buscarPorNombre(texto)));
    }

    public String listarPorTipo(String tipoId) {
        return ejecutar(() -> formatear(servicio.listarPorTipo(entero(tipoId))));
    }

    public String listarPorMarca(String marcaId) {
        return ejecutar(() -> formatear(servicio.listarPorMarca(entero(marcaId))));
    }

    public String listarPorProveedor(String proveedorId) {
        return ejecutar(() -> formatear(servicio.listarPorProveedor(entero(proveedorId))));
    }

    public String listarStockBajo() {
        return ejecutar(() -> formatear(servicio.listarStockBajo()));
    }

    // ---------- Apoyo ----------

    // Transforma los textos recibidos en un objeto Repuesto; no guarda datos.
    private Repuesto construir(int id, String nombre, String tipoId, String marcaId, String proveedorId,
                               String precioCompra, String precioVenta, String stockMinimo) {
        Repuesto r = new Repuesto();
        r.setId(id);
        r.setNombre(nombre);
        r.setTipoId(entero(tipoId));
        r.setMarcaId(entero(marcaId));
        r.setProveedorId(entero(proveedorId));
        r.setPrecioCompra(decimal(precioCompra));
        r.setPrecioVenta(decimal(precioVenta));
        r.setStockMinimo(entero(stockMinimo));
        return r;
    }

    private int entero(String texto) {
        return Integer.parseInt(texto.trim());
    }

    private BigDecimal decimal(String texto) {
        return new BigDecimal(texto.trim());
    }

    // Transforma los objetos devueltos por el Service en líneas para consola.
    private String formatear(List<Repuesto> lista) {
        if (lista.isEmpty()) {
            return "No se encontraron repuestos.";
        }
        StringBuilder sb = new StringBuilder();
        for (Repuesto r : lista) {
            sb.append(r.getId()).append(" | ").append(r.getNombre())
                    .append(" | ").append(r.getTipo()).append(" | ").append(r.getMarca())
                    .append(" | ").append(r.getProveedor())
                    .append(" | compra ").append(r.getPrecioCompra())
                    .append(" | venta ").append(r.getPrecioVenta())
                    .append(" | mínimo ").append(r.getStockMinimo()).append("\n");
        }
        return sb.toString();
    }

    /** Un solo lugar donde se convierten los errores en mensajes. */
    private String ejecutar(Supplier<String> accion) {
        try {
            return accion.get();
        } catch (NumberFormatException e) {
            return "Error: escribe números válidos (ids y cantidades enteros; precios con punto, por ejemplo 25000.50).";
        } catch (ValidacionException e) {
            return "Error: " + e.getMessage();
        } catch (DaoException | ConexionException e) {
            return "Error del sistema: " + e.getMessage();
        }
    }
}