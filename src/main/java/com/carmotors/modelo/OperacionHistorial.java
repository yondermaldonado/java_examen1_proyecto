
package com.carmotors.modelo;

import java.math.BigDecimal;
import java.util.Objects;

public class OperacionHistorial {

    private final int catalogoId;
    private final String nombre;
    private final BigDecimal precioCobrado;
    private final Integer intervaloMeses;

    // CONSTRUCTOR
    public OperacionHistorial(
            int catalogoId,
            String nombre,
            BigDecimal precioCobrado,
            Integer intervaloMeses) {

        if (catalogoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID de la operacion debe ser positivo");
        }

        Objects.requireNonNull(nombre,
                "El nombre es obligatorio");

        if (nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre no puede estar vacio");
        }

        Objects.requireNonNull(precioCobrado,
                "El precio cobrado es obligatorio");

        if (precioCobrado.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio no puede ser negativo");
        }

        if (intervaloMeses != null && intervaloMeses <= 0) {
            throw new IllegalArgumentException(
                    "El intervalo debe ser positivo");
        }

        this.catalogoId = catalogoId;
        this.nombre = nombre.strip();
        this.precioCobrado = precioCobrado;
        this.intervaloMeses = intervaloMeses;
    }

    // GETTERS
    public int getCatalogoId() {
        return catalogoId;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getPrecioCobrado() {
        return precioCobrado;
    }

    public Integer getIntervaloMeses() {
        return intervaloMeses;
    }

    // METODO DE CONSULTA
    public boolean tienePeriodicidad() {
        return intervaloMeses != null;
    }
}
