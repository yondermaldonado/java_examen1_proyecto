package com.carmotors.dao;

import com.carmotors.modelo.OperacionOrden;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;

public interface OperacionOrdenDAO {
    void registrar(Connection con, int ordenId, int catalogoId, BigDecimal precio);
    boolean actualizar(Connection con, int ordenId, int catalogoId, BigDecimal precio);
    boolean quitar(Connection con, int ordenId, int catalogoId);
    List<OperacionOrden> listar(Connection con, int ordenId);
}
