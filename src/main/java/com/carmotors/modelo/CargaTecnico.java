package com.carmotors.modelo;

/** Resumen de asignaciones vigentes de un técnico. */
public record CargaTecnico(int tecnicoId, String nombre, boolean activo, long ordenesEnProceso) {}
