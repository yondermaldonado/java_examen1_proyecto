package com.carmotors.controlador;

import com.carmotors.servicio.AlertasInventarioService;

/**
 * CONTROLLER: la futura vista de consola llama a mostrarReporte().
 * El controlador pide el texto al Service; no realiza consultas SQL.
 */
public class AlertasInventarioController {
    private final AlertasInventarioService servicio;

    public AlertasInventarioController(AlertasInventarioService servicio) {
        this.servicio = servicio;
    }

    // Vista -> Controller -> Service -> texto para imprimir en consola.
    public String mostrarReporte() {
        return servicio.reporteConsola();
    }
}
