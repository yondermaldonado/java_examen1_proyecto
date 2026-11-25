package com.carmotors.modelo;

import com.carmotors.excepcion.ValidacionException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Valores históricos tomados de la orden al facturar. */
public record DatosOrdenParaFactura(List<Repuesto> repuestos, List<ManoObra> manoObra) {
    public DatosOrdenParaFactura {
        if (repuestos == null || manoObra == null
                || repuestos.stream().anyMatch(linea -> linea == null)
                || manoObra.stream().anyMatch(linea -> linea == null)) {
            throw new ValidacionException("Las listas de detalle de la orden no pueden contener datos nulos.");
        }
        repuestos = List.copyOf(repuestos);
        manoObra = List.copyOf(manoObra);
    }

    public record Repuesto(long repuestoId, String nombre, long cantidad, BigDecimal precioUnitarioHistorico) {
        public Repuesto {
            if (repuestoId <= 0 || cantidad <= 0) {
                throw new ValidacionException("El repuesto y su cantidad deben ser positivos.");
            }
            validarNombre(nombre);
            precioUnitarioHistorico = validarImporte(precioUnitarioHistorico, "El precio histórico");
        }

        public BigDecimal importe() {
            return precioUnitarioHistorico.multiply(BigDecimal.valueOf(cantidad));
        }
    }

    public record ManoObra(long servicioCatalogoId, String nombre, BigDecimal precioCobrado) {
        public ManoObra {
            if (servicioCatalogoId <= 0) {
                throw new ValidacionException("El identificador de la operación debe ser positivo.");
            }
            validarNombre(nombre);
            precioCobrado = validarImporte(precioCobrado, "El importe de mano de obra");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionException("El nombre del detalle es obligatorio.");
        }
    }

    private static BigDecimal validarImporte(BigDecimal importe, String campo) {
        if (importe == null || importe.signum() < 0 || importe.stripTrailingZeros().scale() > 2) {
            throw new ValidacionException(campo + " debe ser no negativo y tener hasta dos decimales.");
        }
        return importe.setScale(2, RoundingMode.UNNECESSARY);
    }
}
