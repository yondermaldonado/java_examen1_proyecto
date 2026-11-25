package com.carmotors.servicio;

import com.carmotors.config.ConexionFactory;
import com.carmotors.dao.ConsumoRepuestosDAO;
import com.carmotors.dao.ConsumoRepuestosDAO.Consumo;
import com.carmotors.dao.factory.ConsumoRepuestosDAOFactory;
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

public class ConsumoRepuestosStrategy implements ReporteStrategy {
    private final ConsumoRepuestosDAO dao;
    private final Supplier<Connection> abrirConexion;

    public ConsumoRepuestosStrategy() {
        this(new ConsumoRepuestosDAOFactory().obtenerDAO(), ConexionFactory.getInstancia()::crearConexion);
    }

    ConsumoRepuestosStrategy(ConsumoRepuestosDAO dao, Supplier<Connection> abrirConexion) {
        this.dao = dao;
        this.abrirConexion = abrirConexion;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.CONSUMO_REPUESTOS;
    }

    @Override
    public ReporteDTO generar(FiltroReporte filtro) {
        Objects.requireNonNull(filtro, "El filtro del reporte es obligatorio.");
        try (Connection conexion = abrirConexion.get()) {
            var totales = dao.listarPorPeriodo(conexion, filtro).stream()
                    .collect(Collectors.groupingBy(Consumo::repuesto, Collectors.summingLong(Consumo::unidades)));
            var filas = totales.entrySet().stream()
                    .map(fila -> new FilaReporteDTO(Long.toString(fila.getKey().id()),
                            fila.getKey().nombre(), fila.getValue()))
                    .toList();
            return new ReporteDTO(tipo(), "Consumo de repuestos", filtro, filtro.orden().ordenar(filas));
        } catch (SQLException e) {
            throw new DaoException("No se pudo cerrar la conexión del reporte.", e);
        }
    }
}
