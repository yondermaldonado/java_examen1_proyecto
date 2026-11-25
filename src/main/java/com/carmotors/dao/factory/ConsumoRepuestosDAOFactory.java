package com.carmotors.dao.factory;

import com.carmotors.dao.ConsumoRepuestosDAO;
import com.carmotors.dao.ConsumoRepuestosDAOJdbc;

/** Creador concreto de Factory Method, sin conexiones ni reglas de negocio. */
public final class ConsumoRepuestosDAOFactory extends DAOFactory<ConsumoRepuestosDAO> {
    @Override
    protected ConsumoRepuestosDAO crearDAO() {
        return new ConsumoRepuestosDAOJdbc();
    }
}
