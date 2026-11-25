package com.carmotors.vista;

import java.util.Objects;

/**
 * Contexto de la sesión DEMOSTRATIVA. No constituye autorización segura.
 * tecnicoId permanece null hasta integrar la selección real de técnicos activos.
 */
public record SesionConsola(Rol rol, String usuario, Integer tecnicoId) {
    public enum Rol { ADMINISTRADOR, TECNICO }

    public SesionConsola {
        Objects.requireNonNull(rol, "El rol es obligatorio");
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }
        if (tecnicoId != null && (tecnicoId <= 0 || rol != Rol.TECNICO)) {
            throw new IllegalArgumentException("Solo una sesión técnica puede seleccionar un ID positivo");
        }
    }

    public SesionConsola conTecnico(int id) {
        if (rol != Rol.TECNICO || id <= 0) {
            throw new IllegalArgumentException("Se requiere sesión técnica e ID positivo");
        }
        return new SesionConsola(rol, usuario, id);
    }
}
