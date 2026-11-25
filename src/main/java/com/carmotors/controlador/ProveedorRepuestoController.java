
package com.carmotors.controlador;

import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.ProveedorRepuesto;
import com.carmotors.servicio.ProveedorRepuestoService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ProveedorRepuestoController {

    private final ProveedorRepuestoService servicio;

    // =========================================
    // CONSTRUCTOR
    // =========================================

    public ProveedorRepuestoController(
            ProveedorRepuestoService servicio) {

        this.servicio = Objects.requireNonNull(
                servicio,
                "El servicio del catalogo no puede ser nulo"
        );
    }

    // =========================================
    // CREATE - REGISTRAR ASOCIACION
    // =========================================

    public ProveedorRepuesto registrarAsociacion(
            int proveedorId,
            int repuestoId,
            String costo) {

        ProveedorRepuesto asociacion =
                new ProveedorRepuesto(
                        proveedorId,
                        repuestoId,
                        convertirCosto(costo)
                );

        servicio.registrar(asociacion);

        return asociacion;
    }

    // =========================================
    // READ - BUSCAR ASOCIACION EXACTA
    // =========================================

    public Optional<ProveedorRepuesto> buscarAsociacion(
            int proveedorId,
            int repuestoId) {

        return servicio.buscar(
                proveedorId,
                repuestoId
        );
    }

    // =========================================
    // READ - REPUESTOS POR PROVEEDOR
    // =========================================

    public List<ProveedorRepuesto> listarPorProveedor(
            int proveedorId) {

        return servicio.listarPorProveedor(
                proveedorId
        );
    }

    // =========================================
    // READ - PROVEEDORES POR REPUESTO
    // =========================================

    public List<ProveedorRepuesto> listarPorRepuesto(
            int repuestoId) {

        return servicio.listarPorRepuesto(
                repuestoId
        );
    }

    // =========================================
    // UPDATE - ACTUALIZAR COSTO
    // =========================================

    public boolean actualizarCosto(
            int proveedorId,
            int repuestoId,
            String nuevoCosto) {

        return servicio.actualizarCosto(
                proveedorId,
                repuestoId,
                convertirCosto(nuevoCosto)
        );
    }

    // =========================================
    // CONVERSION DE DATOS DE LA VISTA
    // =========================================

    private BigDecimal convertirCosto(String texto) {

        if (texto == null || texto.isBlank()) {
            throw new ValidacionException(
                    "El costo es obligatorio"
            );
        }

        try {
            return new BigDecimal(texto.strip());

        } catch (NumberFormatException e) {
            throw new ValidacionException(
                    "El costo debe ser un numero valido. "
                            + "Ejemplo: 32000.50"
            );
        }
    }
}
