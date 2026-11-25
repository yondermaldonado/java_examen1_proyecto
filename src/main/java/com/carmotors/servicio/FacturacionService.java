package com.carmotors.servicio;

import com.carmotors.dao.FacturaDAO;
import com.carmotors.dao.factory.FacturaDAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Objects;

/** Calcula y guarda con la conexión de la transición a Facturado. */
public final class FacturacionService implements FacturacionOrden {
    private final FacturaDAO dao;
    private final BigDecimal tasaImpuesto;

    public FacturacionService(BigDecimal tasaImpuesto) {
        this(new FacturaDAOFactory().obtenerDAO(), tasaImpuesto);
    }

    public FacturacionService(FacturaDAO dao, BigDecimal tasaImpuesto) {
        this.dao = Objects.requireNonNull(dao, "El DAO de facturación es obligatorio.");
        this.tasaImpuesto = CalculadorFactura.validarTasa(tasaImpuesto);
    }

    @Override
    public void facturar(Connection conexion, int ordenId, LocalDateTime fechaEmision) {
        if (fechaEmision == null) {
            throw new ValidacionException("La fecha de emisión es obligatoria.");
        }
        try {
            if (conexion.getAutoCommit()) {
                throw new ValidacionException("La facturación requiere la transacción de la orden.");
            }
        } catch (SQLException e) {
            throw new DaoException("No se pudo comprobar la transacción de facturación.", e);
        }
        var calculador = new CalculadorFactura(id -> dao.buscarOrdenCompletada(conexion, id), tasaImpuesto);
        dao.insertar(conexion, calculador.calcular(ordenId), fechaEmision);
    }
}
