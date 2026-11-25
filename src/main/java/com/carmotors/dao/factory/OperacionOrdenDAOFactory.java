package com.carmotors.dao.factory;

import com.carmotors.dao.OperacionOrdenDAO;
import com.carmotors.dao.OperacionOrdenDAOJdbc;

public final class OperacionOrdenDAOFactory extends DAOFactory<OperacionOrdenDAO> {
    @Override protected OperacionOrdenDAO crearDAO() { return new OperacionOrdenDAOJdbc(); }
}
