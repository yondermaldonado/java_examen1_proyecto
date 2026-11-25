package com.carmotors.excepcion;

/** Se lanza al intentar eliminar un cliente que todavia tiene vehiculos asociados. */
public class ClienteConVehiculosException extends RuntimeException {
    public ClienteConVehiculosException(String mensaje) {
        super(mensaje);
    }
}