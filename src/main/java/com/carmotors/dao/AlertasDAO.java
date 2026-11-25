package com.carmotors.dao;

import com.carmotors.modelo.RepuestoMinimo;
import java.sql.Connection;
import java.util.List;

/** DAO: contrato que usa AlertasInventarioService para pedir repuestos y minimos. */
public interface AlertasDAO {
    List<RepuestoMinimo> listarRepuestos(Connection conexion);
}
