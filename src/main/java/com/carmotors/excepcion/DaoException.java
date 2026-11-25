package com.carmotors.excepcion;

/** Error técnico al hablar con la base de datos. La causa guarda el detalle. */
public class DaoException extends RuntimeException {
    public DaoException(String mensaje) {
        super(mensaje);
    }

    public DaoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}