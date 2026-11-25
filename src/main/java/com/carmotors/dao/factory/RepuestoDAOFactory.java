package com.carmotors.dao.factory;

import com.carmotors.dao.RepuestoDAO;
import com.carmotors.dao.RepuestoDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Repuesto. */
public final class RepuestoDAOFactory extends DAOFactory<RepuestoDAO> {
    @Override
    protected RepuestoDAO crearDAO() {
        return new RepuestoDAOJdbc();
    }
}
