package com.carmotors.excepcion;

/** Se lanza cuando el documento o el email del cliente ya estan registrados. */
public class ClienteDuplicadoException extends RuntimeException {
    public ClienteDuplicadoException(String mensaje) {
        super(mensaje);
    }
}