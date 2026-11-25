package com.carmotors.modelo;

import java.util.Locale;

/**
 * Entidad de un vehículo asociado a un cliente y a un modelo del catálogo.
 * La marca se obtiene mediante modelos_vehiculo y marcas_vehiculo.
 */
public class Vehiculo {

    private int id;
    private final String placa;
    private final int clienteId;
    private final int modeloId;

    public Vehiculo(String placa, int clienteId, int modeloId) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }

        String placaNormalizada = placa.strip()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (placaNormalizada.isBlank() || placaNormalizada.length() > 10) {
            throw new IllegalArgumentException("La placa debe tener entre 1 y 10 caracteres");
        }
        if (clienteId <= 0) {
            throw new IllegalArgumentException("El ID del propietario debe ser mayor que cero");
        }
        if (modeloId <= 0) {
            throw new IllegalArgumentException("El ID del modelo debe ser mayor que cero");
        }

        this.placa = placaNormalizada;
        this.clienteId = clienteId;
        this.modeloId = modeloId;
    }

    public void setId(Integer id) {
        if (this.id != 0) {
            throw new IllegalStateException("El vehículo ya tiene un ID asignado");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getPlaca() {
        return placa;
    }

    public int getClienteId() {
        return clienteId;
    }

    public int getModeloId() {
        return modeloId;
    }
}
