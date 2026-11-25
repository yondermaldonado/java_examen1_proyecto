package com.carmotors.dao.factory;

import java.util.Objects;

/** Creador abstracto de Factory Method. No abre ni conserva conexiones JDBC. */
public abstract class DAOFactory<D> {
    /** Operación del creador que delega la construcción al método sobrescrito. */
    public final D obtenerDAO() {
        return Objects.requireNonNull(crearDAO(), "El creador debe devolver un DAO.");
    }

    protected abstract D crearDAO();
}
