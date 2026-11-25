package com.carmotors.modelo;

import java.math.BigDecimal;

/** Una fila del catálogo: un repuesto con UNO de sus proveedores y el costo de ese proveedor. */
// MODELO: almacena los datos de un repuesto.
// Lo construye RepuestoController y lo utilizan RepuestoService y RepuestoDAOJdbc.
// No contiene SQL ni abre conexiones.
public class Repuesto {
    // Datos que representan el repuesto; solo se modifican con setters.
    private int id;
    private String nombre;
    private int tipoId;
    private int marcaId;
    private int proveedorId;
    private BigDecimal precioCompra;
    private BigDecimal precioVenta;
    private int stockMinimo;

    // Solo para mostrar en pantalla: los llena el DAO al leer
    private String tipo;
    private String marca;
    private String proveedor;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public int getTipoId() {
        return tipoId;
    }

    public void setTipoId(int tipoId) {
        this.tipoId = tipoId;
    }

    public int getMarcaId() {
        return marcaId;
    }

    public void setMarcaId(int marcaId) {
        this.marcaId = marcaId;
    }

    public int getProveedorId() {
        return proveedorId;
    }

    public void setProveedorId(int proveedorId) {
        this.proveedorId = proveedorId;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }
}
