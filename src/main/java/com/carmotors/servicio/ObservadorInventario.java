package com.carmotors.servicio;

/** INTERFAZ OBSERVER: el publicador llama a este metodo al avisar un cambio. */
public interface ObservadorInventario {
    void inventarioCambio(int repuestoId);
}
