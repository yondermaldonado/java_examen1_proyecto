package com.carmotors.dao.factory;

import com.carmotors.dao.ClienteVehiculosDAO;
import com.carmotors.dao.ClienteVehiculosDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de ClienteVehiculos. */
public final class ClienteVehiculosDAOFactory extends DAOFactory<ClienteVehiculosDAO> {
    @Override
    protected ClienteVehiculosDAO crearDAO() {
        return new ClienteVehiculosDAOJdbc();
    }
}
