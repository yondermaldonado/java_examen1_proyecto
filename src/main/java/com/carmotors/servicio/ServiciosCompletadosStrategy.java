package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ReporteServiciosDAO;
import com.carmotors.dao.factory.ReporteServiciosDAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.ReporteDTO;
import com.carmotors.modelo.TipoReporte;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class ServiciosCompletadosStrategy
        implements ReporteStrategy {

    private final ReporteServiciosDAO dao;

    public ServiciosCompletadosStrategy() {
        this.dao = new ReporteServiciosDAOFactory().obtenerDAO();
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.SERVICIOS_COMPLETADOS;
    }

    @Override
    public ReporteDTO generar(FiltroReporte filtro) {

        try (Connection conexion =
                     ConexionFactory.getInstancia().crearConexion()) {

            List<FilaReporteDTO> filas =
                    dao.listarCompletados(conexion, filtro);

            return new ReporteDTO(
                    tipo(),
                    "Servicios completados",
                    filtro,
                    filtro.orden().ordenar(filas)
            );

        } catch (SQLException e) {
            throw new DaoException(
                    "Error en el reporte de servicios completados.",
                    e
            );
        }
    }
}