package com.carmotors.excepcion;

/** Se lanza cuando una operación exige que el cliente exista y no se encuentra. */
public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
