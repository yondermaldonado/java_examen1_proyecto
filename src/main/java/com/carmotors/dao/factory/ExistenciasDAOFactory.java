package com.carmotors.dao.factory;

import com.carmotors.dao.ExistenciasDAO;
import com.carmotors.dao.ExistenciasDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Existencias. */
public final class ExistenciasDAOFactory extends DAOFactory<ExistenciasDAO> {
    @Override
    protected ExistenciasDAO crearDAO() {
        return new ExistenciasDAOJdbc();
    }
}
