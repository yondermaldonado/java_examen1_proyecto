
package com.carmotors.modelo;

import java.util.Comparator;
import java.util.List;

public enum OrdenReporte {

    CANTIDAD_DESC,
    CANTIDAD_ASC,
    NOMBRE_ASC;

    public List<FilaReporteDTO> ordenar(List<FilaReporteDTO> filas) {

        Comparator<FilaReporteDTO> porNombre =
                Comparator.comparing(
                        FilaReporteDTO::nombre,
                        String.CASE_INSENSITIVE_ORDER
                ).thenComparing(FilaReporteDTO::codigo);

        Comparator<FilaReporteDTO> comparador = switch (this) {

            case CANTIDAD_DESC ->
                    Comparator.comparingLong(FilaReporteDTO::cantidad)
                            .reversed()
                            .thenComparing(porNombre);

            case CANTIDAD_ASC ->
                    Comparator.comparingLong(FilaReporteDTO::cantidad)
                            .thenComparing(porNombre);

            case NOMBRE_ASC -> porNombre;
        };

        return filas.stream()
                .sorted(comparador)
                .toList();
    }
}
