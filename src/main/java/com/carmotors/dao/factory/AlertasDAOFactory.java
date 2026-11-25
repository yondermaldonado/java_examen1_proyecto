package com.carmotors.dao.factory;

import com.carmotors.dao.AlertasDAO;
import com.carmotors.dao.AlertasDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Alertas. */
public final class AlertasDAOFactory extends DAOFactory<AlertasDAO> {
    @Override
    protected AlertasDAO crearDAO() {
        return new AlertasDAOJdbc();
    }
}
