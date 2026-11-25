package com.carmotors.dao.factory;

import com.carmotors.dao.InventarioReservasJdbc;
import com.carmotors.servicio.InventarioReservas;

public final class InventarioReservasFactory extends DAOFactory<InventarioReservas> {
    @Override protected InventarioReservas crearDAO() { return new InventarioReservasJdbc(); }
}
