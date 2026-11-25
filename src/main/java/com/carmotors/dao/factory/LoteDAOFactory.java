package com.carmotors.dao.factory;

import com.carmotors.dao.LoteDAO;
import com.carmotors.dao.LoteDAOJdbc;

/** Creador concreto para persistencia MySQL/JDBC de Lote. */
public final class LoteDAOFactory extends DAOFactory<LoteDAO> {
    @Override
    protected LoteDAO crearDAO() {
        return new LoteDAOJdbc();
    }
}
