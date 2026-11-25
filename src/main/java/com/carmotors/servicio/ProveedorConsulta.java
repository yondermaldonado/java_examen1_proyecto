package com.carmotors.servicio;

/** Lo único que el módulo Repuesto necesita saber del módulo Proveedor. */
public interface ProveedorConsulta {
    boolean existe(int proveedorId);
}
