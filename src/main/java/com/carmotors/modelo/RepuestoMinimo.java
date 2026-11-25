package com.carmotors.modelo;

/**
 * MODELO: contiene los datos necesarios para revisar el stock minimo.
 * AlertasDAOJdbc lo llena desde MySQL y AlertasInventarioService lo utiliza.
 */
public class RepuestoMinimo {
    private final int id;
    private final String nombre;
    private final int stockMinimo;

    // Constructor normal: recibe los valores encontrados en la tabla repuestos.
    public RepuestoMinimo(int id, String nombre, int stockMinimo) {
        this.id = id;
        this.nombre = nombre;
        this.stockMinimo = stockMinimo;
    }

    // Getters: lectura de datos sin permitir que se cambien accidentalmente.
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public int getStockMinimo() { return stockMinimo; }
}
