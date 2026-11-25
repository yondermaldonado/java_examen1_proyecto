package com.carmotors.modelo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** MODELO: lo crea LoteController, lo valida LoteService y lo guarda LoteDAOJdbc.
 * Un ingreso de mercancía: un código único, un proveedor, una fecha y una o más líneas de repuestos. */
public class Lote {
    private int id;
    private String codigo;
    private int proveedorId;
    private LocalDate fechaIngreso;
    private List<LoteLinea> lineas = new ArrayList<>();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public int getProveedorId() { return proveedorId; }
    public void setProveedorId(int proveedorId) { this.proveedorId = proveedorId; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDate fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public List<LoteLinea> getLineas() { return lineas; }
    public void setLineas(List<LoteLinea> lineas) { this.lineas = lineas; }

    /** Agrega un repuesto al ingreso. No guarda nada aun en MySQL. */
    public void agregarLinea(LoteLinea linea) {
        this.lineas.add(linea);
    }
}