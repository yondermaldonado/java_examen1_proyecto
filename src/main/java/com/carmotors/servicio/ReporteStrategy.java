
package com.carmotors.servicio;

import com.carmotors.modelo.TipoReporte;
import com.carmotors.modelo.FiltroReporte;
import com.carmotors.modelo.ReporteDTO;

public interface ReporteStrategy {

    TipoReporte tipo();

    ReporteDTO generar(FiltroReporte filtro);

}
