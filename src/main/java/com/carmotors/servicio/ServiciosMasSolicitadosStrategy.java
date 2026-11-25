package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ReporteServiciosDAO;
import com.carmotors.dao.factory.ReporteServiciosDAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.ReporteDTO;
import com.carmotors.modelo.SolicitudServicioReporteDato;
import com.carmotors.modelo.TipoReporte;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ServiciosMasSolicitadosStrategy implements ReporteStrategy {
    private final ReporteServiciosDAO dao;
    private final Supplier<Connection> abrirConexion;

    public ServiciosMasSolicitadosStrategy() {
        this(ConexionFactory.getInstancia()::crearConexion);
    }

    ServiciosMasSolicitadosStrategy(Supplier<Connection> abrirConexion) {
        this(new ReporteServiciosDAOFactory().obtenerDAO(), abrirConexion);
    }

    public ServiciosMasSolicitadosStrategy(ReporteServiciosDAO dao, Supplier<Connection> abrirConexion) {
        this.dao = Objects.requireNonNull(dao, "El DAO del reporte es obligatorio.");
        this.abrirConexion = Objects.requireNonNull(abrirConexion, "El proveedor de conexiones es obligatorio.");
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.SERVICIOS_SOLICITADOS;
    }

    @Override
    public ReporteDTO generar(FiltroReporte filtro) {
        Objects.requireNonNull(filtro, "El filtro del reporte es obligatorio.");
        try (Connection conexion = abrirConexion.get()) {
            var conteos = dao.listarSolicitudes(conexion, filtro).stream()
                    .collect(Collectors.groupingBy(
                            SolicitudServicioReporteDato::tipoMantenimiento, Collectors.counting()));
            var filas = conteos.entrySet().stream()
                    .map(tipo -> new FilaReporteDTO(tipo.getKey(), tipo.getKey(), tipo.getValue()))
                    .toList();
            return new ReporteDTO(tipo(), "Servicios más solicitados", filtro, filtro.orden().ordenar(filas));
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión del reporte.", e);
        }
    }
}
