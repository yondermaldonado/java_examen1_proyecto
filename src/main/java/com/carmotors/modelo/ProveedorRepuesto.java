
package com.carmotors.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ProveedorRepuesto {

    private static final BigDecimal COSTO_MAXIMO =
            new BigDecimal("99999999.99");

    private final int proveedorId;
    private final int repuestoId;
    private final BigDecimal costo;

    public ProveedorRepuesto(
            int proveedorId,
            int repuestoId,
            BigDecimal costo
    ) {

        if (proveedorId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del proveedor debe ser mayor que cero"
            );
        }

        if (repuestoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del repuesto debe ser mayor que cero"
            );
        }

        if (costo == null) {
            throw new IllegalArgumentException(
                    "El costo no puede ser nulo"
            );
        }

        if (costo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "El costo no puede ser negativo"
            );
        }

        if (costo.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "El costo puede tener máximo dos decimales"
            );
        }

        if (costo.compareTo(COSTO_MAXIMO) > 0) {
            throw new IllegalArgumentException(
                    "El costo supera el valor máximo permitido"
            );
        }

        this.proveedorId = proveedorId;
        this.repuestoId = repuestoId;

        this.costo = costo.setScale(
                2,
                RoundingMode.UNNECESSARY
        );
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public int getRepuestoId() {
        return repuestoId;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public ProveedorRepuesto conCosto(BigDecimal nuevoCosto) {
        return new ProveedorRepuesto(
                proveedorId,
                repuestoId,
                nuevoCosto
        );
    }
}
