package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ParticipacionesTecnicosDAO;
import com.carmotors.dao.factory.ParticipacionesTecnicosDAOFactory;
import com.carmotors.excepcion.DaoException;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.ReporteDTO;
import com.carmotors.modelo.TipoReporte;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ProductividadTecnicosStrategy implements ReporteStrategy {
    private final ParticipacionesTecnicosDAO asignaciones;
    private final Supplier<Connection> abrirConexion;

    public ProductividadTecnicosStrategy() {
        this(new ParticipacionesTecnicosDAOFactory().obtenerDAO(), ConexionFactory.getInstancia()::crearConexion);
    }

    ProductividadTecnicosStrategy(ParticipacionesTecnicosDAO asignaciones, Supplier<Connection> abrirConexion) {
        this.asignaciones = asignaciones;
        this.abrirConexion = abrirConexion;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.PRODUCTIVIDAD_TECNICOS;
    }

    @Override
    public ReporteDTO generar(FiltroReporte filtro) {
        Objects.requireNonNull(filtro, "El filtro del reporte es obligatorio.");
        try (Connection conexion = abrirConexion.get()) {
            var conteos = asignaciones.listarPorPeriodo(conexion, filtro).stream()
                    .collect(Collectors.groupingBy(tecnico -> tecnico, Collectors.counting()));
            var filas = conteos.entrySet().stream()
                    .map(fila -> new FilaReporteDTO(Long.toString(fila.getKey().id()),
                            fila.getKey().nombre(), fila.getValue()))
                    .toList();
            return new ReporteDTO(tipo(), "Productividad por técnico", filtro, filtro.orden().ordenar(filas));
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión del reporte.", e);
        }
    }
}
