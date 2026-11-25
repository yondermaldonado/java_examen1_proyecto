package com.carmotors.dao;

import com.carmotors.modelo.Vehiculo;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de acceso a datos de vehículos.
 * La conexión y las transacciones son responsabilidad del Service.
 */
public interface VehiculoDAO {

    Vehiculo insertar(Connection conexion, Vehiculo vehiculo);

    Optional<Vehiculo> buscarPorId(Connection conexion, int id);

    Optional<Vehiculo> buscarPorPlaca(Connection conexion, String placa);

    List<Vehiculo> listar(Connection conexion);

    List<Vehiculo> listarPorCliente(Connection conexion, int clienteId);

    boolean actualizar(Connection conexion, Vehiculo vehiculo);

    boolean eliminar(Connection conexion, int id);

    // Consulta del catálogo existente; no administra los modelos.
    boolean existeModelo(Connection conexion, int modeloId);
}
