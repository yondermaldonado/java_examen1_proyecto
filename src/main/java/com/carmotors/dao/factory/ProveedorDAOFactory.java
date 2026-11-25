
package com.carmotors.dao.factory;

import com.carmotors.dao.ProveedorDAO;
import com.carmotors.dao.ProveedorDAOJdbc;

public final class ProveedorDAOFactory
        extends DAOFactory<ProveedorDAO> {

    @Override
    protected ProveedorDAO crearDAO() {
        return new ProveedorDAOJdbc();
    }
}
