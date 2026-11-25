package com.carmotors.modelo;

import java.math.BigDecimal;

public record ServicioCatalogo(int id, String nombre, BigDecimal precioBase, Integer intervaloMeses) {}
