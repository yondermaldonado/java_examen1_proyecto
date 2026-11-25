package com.carmotors.excepcion;

/** Error al acceder a la base de datos. La causa conserva el SQLException original. */
public class AccesoDatosException extends RuntimeException {
    public AccesoDatosException(String mensaje) {
        super(mensaje);
    }

    public AccesoDatosException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}