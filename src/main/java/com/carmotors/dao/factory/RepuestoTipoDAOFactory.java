package com.carmotors.dao.factory;

import com.carmotors.dao.RepuestoTipoDAO;
import com.carmotors.dao.RepuestoTipoDAOJdbc;

/** Factory Method para construir el DAO de tipos de repuesto. */
public final class RepuestoTipoDAOFactory extends DAOFactory<RepuestoTipoDAO> {
    @Override
    protected RepuestoTipoDAO crearDAO() {
        return new RepuestoTipoDAOJdbc();
    }
}
