
package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.HistorialServiciosDAO;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.modelo.ServicioHistorial;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class HistorialServiciosConsultaJdbc
        implements HistorialServiciosConsulta {

    private final HistorialServiciosDAO historialDAO;
    private final Supplier<Connection> conexiones;

    public HistorialServiciosConsultaJdbc(
            HistorialServiciosDAO historialDAO) {

        this(historialDAO, () -> ConexionFactory.getInstancia().crearConexion());
    }

    public HistorialServiciosConsultaJdbc(HistorialServiciosDAO historialDAO,
                                         Supplier<Connection> conexiones) {
        this.conexiones = Objects.requireNonNull(conexiones, "El proveedor de conexiones es obligatorio");
        this.historialDAO = Objects.requireNonNull(
                historialDAO,
                "El DAO de historial es obligatorio"
        );
    }

    @Override
    public List<ServicioHistorial> listarPorVehiculo(
            int vehiculoId) {

        if (vehiculoId <= 0) {
            throw new IllegalArgumentException(
                    "El ID del vehiculo debe ser positivo"
            );
        }

        try (Connection conexion =
                     conexiones.get()) {

            return historialDAO.listarPorVehiculo(
                    conexion,
                    vehiculoId
            );

        } catch (SQLException e) {

            throw new AccesoDatosException(
                    "Error al cerrar la conexion del historial", e
            );
        }
    }
}
