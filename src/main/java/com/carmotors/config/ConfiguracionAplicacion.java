package com.carmotors.config;

import java.math.BigDecimal;
import java.nio.file.Path;

/** Configuración local del ejercicio, sin variables de entorno. */
public final class ConfiguracionAplicacion {
    private ConfiguracionAplicacion() {}
    // Tasa de demostración del proyecto (19 %); no constituye una regla fiscal universal.
    public static final BigDecimal TASA_IMPUESTO = new BigDecimal("0.19");
    public static final Path CARPETA_FACTURAS = Path.of("facturas");
}
