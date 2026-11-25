package com.carmotors.dao.factory;

import com.carmotors.dao.HistorialServiciosDAO;
import com.carmotors.dao.HistorialServiciosDAOJdbc;

/** Factory Method para la implementación JDBC de historial. */
public final class HistorialServiciosDAOFactory extends DAOFactory<HistorialServiciosDAO> {
    @Override
    protected HistorialServiciosDAO crearDAO() {
        return new HistorialServiciosDAOJdbc();
    }
}
