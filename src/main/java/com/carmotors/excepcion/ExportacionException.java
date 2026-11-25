package com.carmotors.excepcion;

/** Un fallo del archivo no revierte una factura ya confirmada en MySQL. */
public final class ExportacionException extends RuntimeException {
    public ExportacionException(String mensaje, Throwable causa) { super(mensaje, causa); }
}
