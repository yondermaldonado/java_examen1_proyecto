package com.carmotors.dao;

import com.carmotors.modelo.OrdenServicio;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Alta y consultas; la Connection y la transacción pertenecen al servicio. */
public interface OrdenServicioDAO {
    Optional<OrdenServicio> bloquearPorId(Connection con, int id);
    boolean actualizarEstado(Connection con, int id, String actual, String nuevo, LocalDateTime fecha);
    boolean existeFactura(Connection con, int id);
    boolean consumoCompleto(Connection con, int id);
    int crear(Connection con, OrdenServicio orden);
    Optional<OrdenServicio> buscarPorId(Connection con, int id);
    List<OrdenServicio> listar(Connection con);
    List<OrdenServicio> listarPorVehiculo(Connection con, int vehiculoId);
    List<OrdenServicio> listarPorEstado(Connection con, String estado);
}
