package com.carmotors.dao.factory;

import com.carmotors.dao.ClienteDAO;
import com.carmotors.dao.ClienteDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Cliente. */
public final class ClienteDAOFactory extends DAOFactory<ClienteDAO> {
    @Override
    protected ClienteDAO crearDAO() {
        return new ClienteDAOJdbc();
    }
}
