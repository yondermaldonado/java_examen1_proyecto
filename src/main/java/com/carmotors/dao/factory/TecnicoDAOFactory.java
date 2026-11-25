package com.carmotors.dao.factory;

import com.carmotors.dao.TecnicoDAO;
import com.carmotors.dao.TecnicoDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Tecnico. */
public final class TecnicoDAOFactory extends DAOFactory<TecnicoDAO> {
    @Override
    protected TecnicoDAO crearDAO() {
        return new TecnicoDAOJdbc();
    }
}
