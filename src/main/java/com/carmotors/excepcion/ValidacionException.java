package com.carmotors.excepcion;

/** Dato inválido o regla de negocio incumplida. El mensaje se puede mostrar al usuario. */
public class ValidacionException extends RuntimeException {
    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}