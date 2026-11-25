package com.carmotors.servicio;

import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.DatosOrdenParaFactura;
import com.carmotors.modelo.FacturaDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class CalculadorFactura {
    private static final int DECIMALES_MONEDA = 2;
    private static final BigDecimal CERO_MONETARIO = BigDecimal.ZERO.setScale(DECIMALES_MONEDA);

    private final ConsultaOrdenCompletada consulta;
    private final BigDecimal tasaImpuesto;

    /** Tasa expresada como fracción (0.19 representa 19 %). */
    public CalculadorFactura(ConsultaOrdenCompletada consulta, BigDecimal tasaImpuesto) {
        this.consulta = Objects.requireNonNull(consulta, "La consulta de órdenes es obligatoria.");
        this.tasaImpuesto = validarTasa(tasaImpuesto);
    }

    static BigDecimal validarTasa(BigDecimal tasaImpuesto) {
        if (tasaImpuesto == null || tasaImpuesto.signum() < 0 || tasaImpuesto.compareTo(BigDecimal.ONE) > 0) {
            throw new ValidacionException("La tasa de impuesto debe estar entre 0 y 1.");
        }
        return tasaImpuesto;
    }

    public FacturaDTO calcular(long ordenId) {
        if (ordenId <= 0) {
            throw new ValidacionException("El identificador de la orden debe ser positivo.");
        }
        DatosOrdenParaFactura detalle = consulta.buscar(ordenId)
                .orElseThrow(() -> new ValidacionException("La orden no existe o no está completada."));

        BigDecimal repuestos = detalle.repuestos().stream()
                .map(DatosOrdenParaFactura.Repuesto::importe)
                .reduce(CERO_MONETARIO, BigDecimal::add);
        BigDecimal manoObra = detalle.manoObra().stream()
                .map(DatosOrdenParaFactura.ManoObra::precioCobrado)
                .reduce(CERO_MONETARIO, BigDecimal::add);
        BigDecimal subtotal = repuestos.add(manoObra);
        BigDecimal impuestos = subtotal.multiply(tasaImpuesto)
                .setScale(DECIMALES_MONEDA, RoundingMode.HALF_UP);

        return new FacturaDTO(ordenId, detalle, repuestos, manoObra, tasaImpuesto,
                impuestos, subtotal.add(impuestos));
    }
}
