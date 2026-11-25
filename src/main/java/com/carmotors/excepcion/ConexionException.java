package com.carmotors.excepcion;

/** Error de configuración o conexión. La causa conserva el detalle técnico para diagnóstico. */
public class ConexionException extends RuntimeException {
    public ConexionException(String mensaje) {
        super(mensaje);
    }

    public ConexionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
