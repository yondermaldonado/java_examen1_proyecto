package com.carmotors.controlador;

import com.carmotors.servicio.OperacionOrdenService;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.stream.Collectors;

/** Adapta el texto de la consola, sin acceso directo a datos. */
public final class OperacionOrdenController {
    private final OperacionOrdenService servicio;
    public OperacionOrdenController(OperacionOrdenService servicio) { this.servicio = Objects.requireNonNull(servicio); }

    public String catalogo() {
        var filas = servicio.listarCatalogo();
        return filas.isEmpty() ? "No hay operaciones en el catálogo." : filas.stream().map(c ->
                "#" + c.id() + " | " + c.nombre() + " | precio base: " + c.precioBase().toPlainString()
                        + " | intervalo meses: " + (c.intervaloMeses() == null ? "No aplica" : c.intervaloMeses()))
                .collect(Collectors.joining(System.lineSeparator()));
    }
    public String listar(String orden) {
        var filas = servicio.listar(Integer.parseInt(orden));
        return filas.isEmpty() ? "Sin operaciones registradas." : filas.stream().map(o ->
                "#" + o.catalogoId() + " | " + o.nombre() + " | cobro: " + o.precioCobrado().toPlainString())
                .collect(Collectors.joining(System.lineSeparator()));
    }
    public void registrar(String orden, String catalogo, String precio) {
        servicio.registrar(Integer.parseInt(orden), Integer.parseInt(catalogo), new BigDecimal(precio));
    }
    public void actualizar(String orden, String catalogo, String precio) {
        servicio.actualizar(Integer.parseInt(orden), Integer.parseInt(catalogo), new BigDecimal(precio));
    }
    public void quitar(String orden, String catalogo) {
        servicio.quitar(Integer.parseInt(orden), Integer.parseInt(catalogo));
    }
}
