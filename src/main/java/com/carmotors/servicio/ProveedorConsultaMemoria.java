package com.carmotors.servicio;

import java.util.Set;

/** Doble en memoria. Reemplazar por la implementación real cuando exista el módulo Proveedor. */
public class ProveedorConsultaMemoria implements ProveedorConsulta {
    private final Set<Integer> ids;

    public ProveedorConsultaMemoria() {
        this(Set.of(1, 2, 3, 4)); // los proveedores de data.sql
    }

    public ProveedorConsultaMemoria(Set<Integer> ids) {
        this.ids = ids;
    }

    @Override
    public boolean existe(int proveedorId) {
        return ids.contains(proveedorId);
    }
}