
package com.carmotors.dao;

import java.sql.Connection;

public interface ClienteVehiculosDAO {

    boolean tieneVehiculos(Connection conexion, int clienteId);

}