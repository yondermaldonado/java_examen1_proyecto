package com.carmotors.config;

import com.carmotors.excepcion.ConexionException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/** Singleton de configuración: cada llamada crea una conexión independiente. */
public final class ConexionFactory {
    private static ConexionFactory instancia;
    private static final String URL = "jdbc:mysql://localhost:3306/carmotors_test?sslMode=DISABLED&allowPublicKeyRetrieval=true&connectTimeout=5000&socketTimeout=10000";
    private static final String USUARIO = "carmotors_admin";
    private static final String CLAVE = "CambiarEstaClave123!";

    private ConexionFactory() {
    }

    public static synchronized ConexionFactory getInstancia() {
        if (instancia == null) {
            instancia = new ConexionFactory();
        }
        return instancia;
    }

    /** El servicio que la solicita debe cerrar la conexión y controlar su transacción. */
    public Connection crearConexion() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (SQLException e) {
            throw new ConexionException("No se pudo conectar a MySQL. Revisa que el servidor local "
                    + "esté iniciado, la base exista y las credenciales sean correctas.", e);
        }
    }

}
