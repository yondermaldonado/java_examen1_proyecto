
package com.carmotors.dao.factory;

import com.carmotors.dao.ProveedorRepuestoDAO;
import com.carmotors.dao.ProveedorRepuestoDAOJdbc;

/**
 * Factory Method para el catálogo de
 * proveedores y repuestos.
 *
 * Crea la implementación JDBC del DAO.
 */
public final class ProveedorRepuestoDAOFactory
        extends DAOFactory<ProveedorRepuestoDAO> {

    @Override
    protected ProveedorRepuestoDAO crearDAO() {

        return new ProveedorRepuestoDAOJdbc();
    }
}
