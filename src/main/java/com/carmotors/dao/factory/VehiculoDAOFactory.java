package com.carmotors.dao.factory;

import com.carmotors.dao.VehiculoDAO;
import com.carmotors.dao.VehiculoDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Vehiculo. */
public final class VehiculoDAOFactory extends DAOFactory<VehiculoDAO> {
    @Override
    protected VehiculoDAO crearDAO() {
        return new VehiculoDAOJdbc();
    }
}
