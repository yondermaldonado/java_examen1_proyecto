
package com.carmotors.modelo;

import java.util.Locale;

public class Proveedor {

    private int id;
    private final String nombre;
    private final String contacto;
    private final String email;

    public Proveedor(String nombre, String contacto, String email) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException(
                    "El nombre del proveedor es obligatorio"
            );
        }

        String nombreLimpio = nombre.strip();
        String contactoLimpio = limpiarOpcional(contacto);
        String emailLimpio = limpiarOpcional(email);

        if (emailLimpio != null) {
            emailLimpio = emailLimpio.toLowerCase(Locale.ROOT);
        }

        if (nombreLimpio.length() > 255) {
            throw new IllegalArgumentException(
                    "El nombre supera los 255 caracteres"
            );
        }

        if (contactoLimpio != null
                && contactoLimpio.length() > 255) {
            throw new IllegalArgumentException(
                    "El contacto supera los 255 caracteres"
            );
        }

        if (emailLimpio != null) {

            if (emailLimpio.length() > 150) {
                throw new IllegalArgumentException(
                        "El correo supera los 150 caracteres"
                );
            }

            if (!emailLimpio.matches(
                    "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            )) {
                throw new IllegalArgumentException(
                        "El formato del correo es invalido"
                );
            }
        }

        if (contactoLimpio == null && emailLimpio == null) {
            throw new IllegalArgumentException(
                    "El proveedor debe tener al menos un contacto o correo"
            );
        }

        this.nombre = nombreLimpio;
        this.contacto = contactoLimpio;
        this.email = emailLimpio;
    }

    private static String limpiarOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.strip();
    }

    public void setId(Integer id) {
        if (this.id != 0) {
            throw new IllegalStateException(
                    "El proveedor ya tiene un ID asignado"
            );
        }

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor que cero"
            );
        }

        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getContacto() {
        return contacto;
    }

    public String getEmail() {
        return email;
    }
}
