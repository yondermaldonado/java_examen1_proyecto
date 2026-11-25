package com.carmotors.servicio;

import com.carmotors.excepcion.ValidacionException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Duraciones en horas y minutos, incluso superiores a 24 horas. */
public final class TiempoTrabajo {
    public static final int MAXIMO_MINUTOS = 999 * 60 + 59;
    private TiempoTrabajo() {}

    public static int leerMinutos(String texto) {
        if (texto == null || !texto.trim().matches("[0-9]{1,3}:[0-5][0-9]")) {
            throw new ValidacionException("Escribe el tiempo como HH:mm, por ejemplo 02:30; los minutos van de 00 a 59.");
        }
        String[] partes = texto.trim().split(":");
        return Integer.parseInt(partes[0]) * 60 + Integer.parseInt(partes[1]);
    }

    public static int minutos(BigDecimal horas) {
        return horas.multiply(BigDecimal.valueOf(60)).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    public static BigDecimal horas(int minutos) {
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public static String formatear(BigDecimal horas) {
        int minutos = minutos(horas);
        return String.format(java.util.Locale.ROOT, "%02d:%02d", minutos / 60, minutos % 60);
    }
}
