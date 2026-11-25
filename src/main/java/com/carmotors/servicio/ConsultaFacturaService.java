package com.carmotors.servicio;

import com.carmotors.dao.FacturaDAO;
import com.carmotors.excepcion.DaoException;
import com.carmotors.excepcion.ValidacionException;
import com.carmotors.modelo.DocumentoFactura;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;

public final class ConsultaFacturaService {
    private final FacturaDAO dao;
    private final Supplier<Connection> conexiones;
    private final ExportadorFacturaTxt exportador;

    public ConsultaFacturaService(FacturaDAO dao, Supplier<Connection> conexiones, ExportadorFacturaTxt exportador) {
        this.dao = Objects.requireNonNull(dao);
        this.conexiones = Objects.requireNonNull(conexiones);
        this.exportador = Objects.requireNonNull(exportador);
    }

    public DocumentoFactura consultar(long ordenId) {
        if (ordenId <= 0) throw new ValidacionException("El número de orden debe ser positivo.");
        try (Connection con = conexiones.get()) {
            return dao.buscarPorOrden(con, ordenId)
                    .orElseThrow(() -> new ValidacionException("La orden no tiene una factura registrada."));
        } catch (SQLException e) { throw new DaoException("No se pudo cerrar la consulta de factura.", e); }
    }

    public String mostrar(long ordenId) { return exportador.contenido(consultar(ordenId)); }
    public Path exportar(long ordenId, Path directorio) { return exportador.exportar(consultar(ordenId), directorio); }
}
