package com.carmotors.dao.factory;

import com.carmotors.dao.ParticipacionesTecnicosDAO;
import com.carmotors.dao.ParticipacionesTecnicosDAOJdbc;

/** Creador concreto de Factory Method, sin conexiones ni reglas de negocio. */
public final class ParticipacionesTecnicosDAOFactory extends DAOFactory<ParticipacionesTecnicosDAO> {
    @Override
    protected ParticipacionesTecnicosDAO crearDAO() {
        return new ParticipacionesTecnicosDAOJdbc();
    }
}
