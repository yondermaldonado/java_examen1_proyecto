package com.carmotors.dao.factory;

import com.carmotors.dao.ServicioTecnicoDAO;
import com.carmotors.dao.ServicioTecnicoDAOJdbc;

public final class ServicioTecnicoDAOFactory extends DAOFactory<ServicioTecnicoDAO> {
    @Override protected ServicioTecnicoDAO crearDAO() { return new ServicioTecnicoDAOJdbc(); }
}
