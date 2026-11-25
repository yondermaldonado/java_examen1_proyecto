package com.carmotors.modelo;

/**
 * MODELO: representa un tipo de repuesto de la tabla repuesto_tipos.
 * El DAO crea estos objetos al leer filas de MySQL.
 * El Controller usa sus getters para mostrar los datos en consola.
 */
public class RepuestoTipo {

    private final int id;
    private final String nombre;

    // Recibe los valores del registro consultado en MySQL.
    public RepuestoTipo(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    // Devuelve el identificador del tipo de repuesto.
    public int getId() {
        return id;
    }

    // Devuelve el nombre del tipo de repuesto.
    public String getNombre() {
        return nombre;
    }
}
