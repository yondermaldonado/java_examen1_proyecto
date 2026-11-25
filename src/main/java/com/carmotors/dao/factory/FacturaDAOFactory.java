package com.carmotors.dao.factory;

import com.carmotors.dao.FacturaDAO;
import com.carmotors.dao.FacturaDAOJdbc;

/** Creador concreto de Factory Method, sin conexiones ni reglas de negocio. */
public final class FacturaDAOFactory extends DAOFactory<FacturaDAO> {
    @Override
    protected FacturaDAO crearDAO() {
        return new FacturaDAOJdbc();
    }
}
