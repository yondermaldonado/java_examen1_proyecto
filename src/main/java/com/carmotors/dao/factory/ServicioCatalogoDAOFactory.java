package com.carmotors.dao.factory;

import com.carmotors.dao.ServicioCatalogoDAO;
import com.carmotors.dao.ServicioCatalogoDAOJdbc;

public final class ServicioCatalogoDAOFactory extends DAOFactory<ServicioCatalogoDAO> {
    @Override protected ServicioCatalogoDAO crearDAO() { return new ServicioCatalogoDAOJdbc(); }
}
