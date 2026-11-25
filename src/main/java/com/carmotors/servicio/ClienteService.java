package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ClienteDAO;
import com.carmotors.dao.ClienteVehiculosDAO;
import com.carmotors.excepcion.AccesoDatosException;
import com.carmotors.excepcion.ClienteConVehiculosException;
import com.carmotors.excepcion.ClienteDuplicadoException;
import com.carmotors.excepcion.ClienteNoEncontradoException;
import com.carmotors.modelo.Cliente;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class ClienteService {

    private final ClienteDAO clienteDAO;
    private final ClienteVehiculosDAO clienteVehiculosDAO;

    public ClienteService(
            ClienteDAO clienteDAO,
            ClienteVehiculosDAO clienteVehiculosDAO) {

        this.clienteDAO = Objects.requireNonNull(
                clienteDAO, "El DAO de clientes no puede ser null");
        this.clienteVehiculosDAO = Objects.requireNonNull(
                clienteVehiculosDAO, "El DAO de vehículos no puede ser null");
    }

    public Cliente registrar(Cliente cliente) {
        Objects.requireNonNull(cliente, "El cliente no puede ser null");

        if (cliente.getId() != 0) {
            throw new IllegalArgumentException(
                    "El cliente ya tiene un ID asignado");
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            conexion.setAutoCommit(false);

            try {
                if (clienteDAO.buscarPorDocumento(
                        conexion, cliente.getDocumento()).isPresent()) {
                    throw new ClienteDuplicadoException(
                            "El documento ya está registrado");
                }

                if (cliente.getEmail() != null
                        && clienteDAO.buscarPorEmail(
                        conexion, cliente.getEmail()).isPresent()) {
                    throw new ClienteDuplicadoException(
                            "El correo electrónico ya está registrado");
                }

                // NO pasar el objeto original al DAO: si posteriormente
                // ocurre un rollback, el objeto original debe conservar ID 0.
                Cliente copiaParaInsertar = new Cliente(
                        cliente.getNombre(),
                        cliente.getDocumento(),
                        cliente.getTelefono(),
                        cliente.getEmail()
                );

                Cliente insertado;
                try {
                    insertado = clienteDAO.insertar(conexion, copiaParaInsertar);
                } catch (AccesoDatosException e) {
                    if (esErrorMysql(e, 1062)) {
                        throw new ClienteDuplicadoException(
                                "El documento o correo ya está registrado");
                    }
                    throw e;
                }

                // Primero confirmar el INSERT; solo después asignar
                // el ID al objeto que recibió quien llamó al servicio.
                conexion.commit();
                cliente.setId(insertado.getId());
                return cliente;

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException(
                        "Error al confirmar el registro", e);

            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la conexión", e);
        }
    }

    public Cliente obtenerPorId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor que cero");
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            return clienteDAO.buscarPorId(conexion, id)
                    .orElseThrow(() -> new ClienteNoEncontradoException(
                            "No existe un cliente con ID: " + id));

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al consultar el cliente", e);
        }
    }

    public Optional<Cliente> buscarPorDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            throw new IllegalArgumentException(
                    "El documento no puede estar vacío");
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            return clienteDAO.buscarPorDocumento(conexion, documento.strip());

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar cliente por documento", e);
        }
    }

    public Optional<Cliente> buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "El correo electrónico no puede estar vacío");
        }

        String emailNormalizado = email.strip().toLowerCase(Locale.ROOT);

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            return clienteDAO.buscarPorEmail(conexion, emailNormalizado);

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al buscar cliente por email", e);
        }
    }

    public List<Cliente> listar() {
        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            return clienteDAO.listar(conexion);

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al listar los clientes", e);
        }
    }

    public Cliente actualizar(Cliente cliente) {
        Objects.requireNonNull(cliente, "El cliente no puede ser null");

        int id = cliente.getId();
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El cliente debe tener un ID válido");
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            conexion.setAutoCommit(false);

            try {
                if (clienteDAO.buscarPorId(conexion, id).isEmpty()) {
                    throw new ClienteNoEncontradoException(
                            "No existe un cliente con ID: " + id);
                }

                Optional<Cliente> clienteConDocumento =
                        clienteDAO.buscarPorDocumento(
                                conexion, cliente.getDocumento());

                if (clienteConDocumento.isPresent()
                        && clienteConDocumento.get().getId() != id) {
                    throw new ClienteDuplicadoException(
                            "El documento pertenece a otro cliente");
                }

                if (cliente.getEmail() != null) {
                    Optional<Cliente> clienteConEmail =
                            clienteDAO.buscarPorEmail(
                                    conexion, cliente.getEmail());

                    if (clienteConEmail.isPresent()
                            && clienteConEmail.get().getId() != id) {
                        throw new ClienteDuplicadoException(
                                "El correo pertenece a otro cliente");
                    }
                }

                try {
                    boolean actualizado = clienteDAO.actualizar(
                            conexion, cliente);

                    if (!actualizado) {
                        throw new ClienteNoEncontradoException(
                                "El cliente ya no existe: " + id);
                    }
                } catch (AccesoDatosException e) {
                    if (esErrorMysql(e, 1062)) {
                        throw new ClienteDuplicadoException(
                                "El documento o correo ya está registrado");
                    }
                    throw e;
                }

                conexion.commit();
                return cliente;

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException(
                        "Error al confirmar la actualización", e);

            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la conexión", e);
        }
    }

    public void eliminar(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException(
                    "El ID debe ser mayor que cero");
        }

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            conexion.setAutoCommit(false);

            try {
                if (clienteDAO.buscarPorId(conexion, id).isEmpty()) {
                    throw new ClienteNoEncontradoException(
                            "No existe un cliente con ID: " + id);
                }

                if (clienteVehiculosDAO.tieneVehiculos(conexion, id)) {
                    throw new ClienteConVehiculosException(
                            "No se puede eliminar un cliente con vehículos asociados");
                }

                // La restricción FK sigue vigente en MySQL.
                // Si otra FK bloquea el DELETE, el DAO conserva
                // su AccesoDatosException con la causa SQL original.
                boolean eliminado = clienteDAO.eliminar(conexion, id);

                if (!eliminado) {
                    throw new ClienteNoEncontradoException(
                            "El cliente ya no existe: " + id);
                }

                conexion.commit();

            } catch (SQLException e) {
                revertir(conexion, e);
                throw new AccesoDatosException(
                        "Error al confirmar la eliminación", e);

            } catch (RuntimeException e) {
                revertir(conexion, e);
                throw e;
            }

        } catch (SQLException e) {
            throw new AccesoDatosException(
                    "Error al gestionar la conexión", e);
        }
    }

    // AUXILIARES
    private boolean esErrorMysql(AccesoDatosException e, int codigo) {
        return e.getCause() instanceof SQLException sql
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
