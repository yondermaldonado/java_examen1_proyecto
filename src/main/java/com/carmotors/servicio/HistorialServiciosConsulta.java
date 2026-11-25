
package com.carmotors.servicio;

import com.carmotors.modelo.ServicioHistorial;
import java.util.List;

public interface HistorialServiciosConsulta {

    List<ServicioHistorial> listarPorVehiculo(int vehiculoId);

}
