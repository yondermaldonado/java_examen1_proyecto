package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ClienteDAO;
import com.carmotors.dao.VehiculoDAO;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ClienteNoEncontradoException;
import com.carmotors.excepcion.VehiculoDuplicadoException;
import com.carmotors.excepcion.VehiculoNoEncontradoException;
import com.carmotors.modelo.Vehiculo;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Reglas de negocio y transacciones del módulo de vehículos. */
public class VehiculoService {

    private final VehiculoDAO vehiculoDAO;
    private final ClienteDAO clienteDAO;

    public VehiculoService(VehiculoDAO vehiculoDAO, ClienteDAO clienteDAO) {
        this.vehiculoDAO = Objects.requireNonNull(
                vehiculoDAO, "El DAO de vehículos no puede ser null");
        this.clienteDAO = Objects.requireNonNull(
                clienteDAO, "El DAO de clientes no puede ser null");
    }

    // CREATE
    public Vehiculo registrar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "El vehículo no puede ser null");
        if (vehiculo.getId() != 0) {
            throw new IllegalArgumentException("El vehículo ya tiene un ID asignado");
        }

        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            conexion.setAutoCommit(false);
            try {
                validarReferencias(conexion, vehiculo);
                validarPlacaUnica(conexion, vehiculo.getPlaca(), 0);

                // El DAO asigna un ID provisional a la copia, no al original.
                Vehiculo copia = new Vehiculo(
                        vehiculo.getPlaca(),
                        vehiculo.getClienteId(),
                        vehiculo.getModeloId());

                Vehiculo insertado;
                try {
                    insertado = vehiculoDAO.insertar(conexion, copia);
                } catch (AccesoDatosException e) {
                    throw traducirErrorEscritura(e);
                }

                conexion.commit();
                vehiculo.setId(insertado.getId());
                return vehiculo;

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException("Error al confirmar el registro del vehículo", e);
            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al gestionar la conexión", e);
        }
    }

    // READ
    public Vehiculo obtenerPorId(int id) {
        validarId(id);
        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            return vehiculoDAO.buscarPorId(conexion, id)
                    .orElseThrow(() -> new VehiculoNoEncontradoException(
                            "No existe un vehículo con ID: " + id));
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al consultar el vehículo", e);
        }
    }

    public Optional<Vehiculo> buscarPorPlaca(String placa) {
        String placaNormalizada = normalizarPlaca(placa);
        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            return vehiculoDAO.buscarPorPlaca(conexion, placaNormalizada);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al buscar el vehículo por placa", e);
        }
    }

    public List<Vehiculo> listar() {
        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            return vehiculoDAO.listar(conexion);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar los vehículos", e);
        }
    }

    public List<Vehiculo> listarPorCliente(int clienteId) {
        validarId(clienteId);
        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            // Si el ID no corresponde a un cliente, se informa explícitamente.
            validarPropietario(conexion, clienteId);
            return vehiculoDAO.listarPorCliente(conexion, clienteId);
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al listar vehículos del cliente", e);
        }
    }

    // UPDATE
    public Vehiculo actualizar(Vehiculo vehiculo) {
        Objects.requireNonNull(vehiculo, "El vehículo no puede ser null");
        validarId(vehiculo.getId());

        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            conexion.setAutoCommit(false);
            try {
                int id = vehiculo.getId();
                if (vehiculoDAO.buscarPorId(conexion, id).isEmpty()) {
                    throw new VehiculoNoEncontradoException(
                            "No existe un vehículo con ID: " + id);
                }

                validarReferencias(conexion, vehiculo);
                validarPlacaUnica(conexion, vehiculo.getPlaca(), id);

                try {
                    if (!vehiculoDAO.actualizar(conexion, vehiculo)) {
                        throw new VehiculoNoEncontradoException(
                                "El vehículo ya no existe: " + id);
                    }
                } catch (AccesoDatosException e) {
                    throw traducirErrorEscritura(e);
                }

                conexion.commit();
                return vehiculo;

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException("Error al confirmar la actualización", e);
            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al gestionar la conexión", e);
        }
    }

    // DELETE
    public void eliminar(int id) {
        validarId(id);
        try (Connection conexion = ConexionFactory.getInstancia().crearConexion()) {
            conexion.setAutoCommit(false);
            try {
                if (vehiculoDAO.buscarPorId(conexion, id).isEmpty()) {
                    throw new VehiculoNoEncontradoException(
                            "No existe un vehículo con ID: " + id);
                }

                try {
                    if (!vehiculoDAO.eliminar(conexion, id)) {
                        throw new VehiculoNoEncontradoException(
                                "El vehículo ya no existe: " + id);
                    }
                } catch (AccesoDatosException e) {
                    if (esErrorMysql(e, 1451)) {
                        throw new AccesoDatosException(
                                "No se puede eliminar el vehículo: tiene registros relacionados", e);
                    }
                    throw e;
                }

                conexion.commit();

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException("Error al confirmar la eliminación", e);
            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesoDatosException("Error al gestionar la conexión", e);
        }
    }

    // VALIDACIONES Y AUXILIARES
    private void validarReferencias(Connection conexion, Vehiculo vehiculo) {
        validarPropietario(conexion, vehiculo.getClienteId());
        if (!vehiculoDAO.existeModelo(conexion, vehiculo.getModeloId())) {
            throw new IllegalArgumentException(
                    "No existe un modelo de vehículo con ID: " + vehiculo.getModeloId());
        }
    }

    private void validarPropietario(Connection conexion, int clienteId) {
        if (clienteDAO.buscarPorId(conexion, clienteId).isEmpty()) {
            throw new ClienteNoEncontradoException(
                    "No existe un cliente propietario con ID: " + clienteId);
        }
    }

    private void validarPlacaUnica(Connection conexion, String placa, int idActual) {
        Optional<Vehiculo> encontrado = vehiculoDAO.buscarPorPlaca(conexion, placa);
        if (encontrado.isPresent() && encontrado.get().getId() != idActual) {
            throw new VehiculoDuplicadoException("La placa ya está registrada: " + placa);
        }
    }

    private String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            throw new IllegalArgumentException("La placa es obligatoria");
        }
        String resultado = placa.strip().replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);
        if (resultado.isEmpty() || resultado.length() > 10) {
            throw new IllegalArgumentException(
                    "La placa debe tener entre 1 y 10 caracteres");
        }
        return resultado;
    }

    private void validarId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
    }

    private RuntimeException traducirErrorEscritura(AccesoDatosException error) {
        if (esErrorMysql(error, 1062)) {
            return new VehiculoDuplicadoException("La placa ya está registrada");
        }
        if (esErrorMysql(error, 1452)) {
            return new AccesoDatosException(
                    "El propietario o modelo dejó de existir durante la operación", error);
        }
        return error;
    }

    private boolean esErrorMysql(AccesoDatosException error, int codigo) {
        return error.getCause() instanceof SQLException sql
                && sql.getErrorCode() == codigo;
    }

    private void revertir(Connection conexion, Exception causa) {
        try {
            conexion.rollback();
        } catch (SQLException errorRollback) {
            causa.addSuppressed(errorRollback);
        }
    }
}
