package com.carmotors.dao;

import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.FilaReporteDTO;
import com.carmotors.modelo.SolicitudServicioReporteDato;

import java.sql.Connection;
import java.util.List;

public interface ReporteServiciosDAO {

    List<SolicitudServicioReporteDato> listarSolicitudes(
            Connection conexion,
            FiltroReporte filtro
    );

    // Nuevo método.
    List<FilaReporteDTO> listarCompletados(
            Connection conexion,
            FiltroReporte filtro
    );
}