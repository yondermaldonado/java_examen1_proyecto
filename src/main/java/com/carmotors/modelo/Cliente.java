package com.carmotors.modelo;

import java.util.Locale;

public class Cliente {
    private int id ;
    private String nombre ;
    private String documento;
    private String telefono;
    private String email;

    public Cliente(String nombre, String documento, String telefono, String email) {

        if (nombre == null || documento == null || telefono == null) {
            throw new IllegalArgumentException("No se pueden agregar datos de identificacion o de contacto null a el cliente");
        }

        if (nombre.isBlank() || documento.isBlank() || telefono.isBlank()) {
            throw new IllegalArgumentException("Alguno de los datos obligatorios del cliente esta vacio");
        }

        String nombreLimpio = nombre.strip().toUpperCase(Locale.ROOT);
        String documentoLimpio = documento.strip();
        String telefonoLimpio = telefono.strip();

        String emailLimpio = null;
        if (email != null && !email.isBlank()) {
            emailLimpio = email.strip().toLowerCase(Locale.ROOT);
        }

        if (nombreLimpio.length() > 255) {
            throw new IllegalArgumentException("El nombre de el cliente no puede tener mas de 255 caracteres");
        }
        if (documentoLimpio.length() > 20) {
            throw new IllegalArgumentException("El documento de identidad de el cliente no puede tener mas de 20 caracteres");
        }
        if (telefonoLimpio.length() > 20) {
            throw new IllegalArgumentException("El numero de telefono de el cliente no puede tener mas de 20 caracteres");
        }
        if (emailLimpio != null && emailLimpio.length() > 100) {
            throw new IllegalArgumentException("El email de el cliente no puede tener mas de 100 caracteres");
        }

        this.nombre = nombreLimpio;
        this.documento = documentoLimpio;
        this.telefono = telefonoLimpio;
        this.email = emailLimpio;
    }
    public void setId (Integer id){
        if (this.id != 0) {
            throw new IllegalStateException("El cliente ya tiene un ID asignado");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("El ID no puede ser nulo ni menor o igual a cero");
        }
        this.id = id;
    }
    public int getId() {
        return this.id;
    }
    public String getNombre() {
        return this.nombre;
    }
    public String getDocumento() {
        return this.documento;
    }

    public String getTelefono() {
        return this.telefono;
    }

    public String getEmail() {
        return email;
    }
}