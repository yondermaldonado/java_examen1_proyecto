package com.carmotors.dao.factory;

import com.carmotors.dao.EvaluacionProveedorDAO;
import com.carmotors.dao.EvaluacionProveedorDAOJdbc;

/** Creador concreto del DAO de evaluaciones de proveedores. */
public final class EvaluacionProveedorDAOFactory
        extends DAOFactory<EvaluacionProveedorDAO> {

    @Override
    protected EvaluacionProveedorDAO crearDAO() {
        return new EvaluacionProveedorDAOJdbc();
    }
}
