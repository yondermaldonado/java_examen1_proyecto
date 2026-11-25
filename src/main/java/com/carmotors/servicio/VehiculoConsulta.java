
package com.carmotors.servicio;

import com.carmotors.modelo.Vehiculo;
import java.util.Optional;

/** Contrato de solo lectura que puede utilizar el modulo de ordenes. */
public interface VehiculoConsulta {

    Optional<Vehiculo> buscarPorId(int id);

    Optional<Vehiculo> buscarPorPlaca(String placa);
}
