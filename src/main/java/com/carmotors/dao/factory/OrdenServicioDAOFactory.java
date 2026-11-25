package com.carmotors.dao.factory;

import com.carmotors.dao.OrdenServicioDAO;
import com.carmotors.dao.OrdenServicioDAOJdbc;

public final class OrdenServicioDAOFactory extends DAOFactory<OrdenServicioDAO> {
    @Override protected OrdenServicioDAO crearDAO() {
        return new OrdenServicioDAOJdbc();
    }
}
