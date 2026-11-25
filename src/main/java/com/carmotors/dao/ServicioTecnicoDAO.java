package com.carmotors.dao;

import com.carmotors.modelo.ServicioTecnico;
import com.carmotors.modelo.CargaTecnico;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/** Persistencia de asignaciones y consultas de sus referencias para bloqueo transaccional. */
public interface ServicioTecnicoDAO {
    Optional<String> bloquearEstadoOrden(Connection con, int servicioId);
    Optional<Boolean> bloquearTecnicoActivo(Connection con, int tecnicoId);
    Optional<BigDecimal> bloquearHoras(Connection con, int servicioId, int tecnicoId);
    List<CargaTecnico> listarCargaEnProceso(Connection con);
    List<CargaTecnico> listarSinOrdenesAbiertas(Connection con);
    boolean existe(Connection con, int servicioId, int tecnicoId);
    void iniciarOrden(Connection con, int servicioId);
    void asignar(Connection con, int servicioId, int tecnicoId);
    boolean quitar(Connection con, int servicioId, int tecnicoId);
    boolean actualizarHoras(Connection con, int servicioId, int tecnicoId, BigDecimal horas);
    List<ServicioTecnico> listarPorOrden(Connection con, int servicioId);
    List<ServicioTecnico> listarPorTecnico(Connection con, int tecnicoId);
}
