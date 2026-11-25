
package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.VehiculoDAO;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Adaptador del contrato de consulta al DAO existente. */
public class VehiculoConsultaImpl implements VehiculoConsulta {

    private final VehiculoDAO vehiculoDAO;

    public VehiculoConsultaImpl(VehiculoDAO vehiculoDAO) {
        this.vehiculoDAO = Objects.requireNonNull(
                vehiculoDAO, "El DAO de vehiculos no puede ser null");
    }

    @Override
    public Optional<Vehiculo> buscarPorId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }

        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            return vehiculoDAO.buscarPorId(conexion, id);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al consultar vehiculo por ID", e);
        }
    }

    @Override
    public Optional<Vehiculo> buscarPorPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }

        String placaNormalizada = placa.strip()
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (placaNormalizada.isEmpty() || placaNormalizada.length() > 10) {
            throw new IllegalArgumentException("La placa debe tener entre 1 y 10 caracteres");
        }

        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            return vehiculoDAO.buscarPorPlaca(conexion, placaNormalizada);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al consultar vehiculo por placa", e);
        }
    }
}
