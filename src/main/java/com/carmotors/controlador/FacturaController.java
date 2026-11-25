package com.carmotors.controlador;

import com.carmotors.servicio.ConsultaFacturaService;
import java.nio.file.Path;
import java.util.Objects;

public final class FacturaController {
    private final ConsultaFacturaService servicio;
    public FacturaController(ConsultaFacturaService servicio) { this.servicio = Objects.requireNonNull(servicio); }
    public String consultar(String orden) { return servicio.mostrar(Long.parseLong(orden)); }
    public String exportar(String orden, String carpeta) {
        return servicio.exportar(Long.parseLong(orden), Path.of(carpeta)).toString();
    }
}
