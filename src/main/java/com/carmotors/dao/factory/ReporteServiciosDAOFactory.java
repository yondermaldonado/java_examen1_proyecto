package com.carmotors.dao.factory;

import com.carmotors.dao.ReporteServiciosDAO;
import com.carmotors.dao.ReporteServiciosDAOJdbc;

/** Creador concreto de Factory Method, sin conexiones ni reglas de negocio. */
public final class ReporteServiciosDAOFactory extends DAOFactory<ReporteServiciosDAO> {
    @Override
    protected ReporteServiciosDAO crearDAO() {
        return new ReporteServiciosDAOJdbc();
    }
}
