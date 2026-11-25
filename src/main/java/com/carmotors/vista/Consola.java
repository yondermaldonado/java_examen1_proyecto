package com.carmotors.vista;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Entrada/salida reutilizable de la aplicación de consola.
 * No conoce controladores, servicios, SQL ni reglas de negocio.
 * Se comparte una sola instancia de Scanner entre todas las vistas.
 */
public final class Consola {
    private final Scanner entrada;
    private final PrintStream salida;

    public Consola(Scanner entrada, PrintStream salida) {
        this.entrada = Objects.requireNonNull(entrada, "La entrada es obligatoria");
        this.salida = Objects.requireNonNull(salida, "La salida es obligatoria");
    }

    public void mostrar(String mensaje) {
        salida.println(mensaje);
    }

    public void titulo(String titulo) {
        salida.println();
        salida.println("=".repeat(65));
        salida.println("  " + titulo);
        salida.println("=".repeat(65));
    }

    /** Devuelve null si se cierra la entrada (EOF), sin bloquear ni lanzar excepción. */
    public String leerTexto(String indicacion) {
        salida.print(indicacion + ": ");
        salida.flush();
        return entrada.hasNextLine() ? entrada.nextLine().strip() : null;
    }

    /**
     * Solo admite las opciones indicadas; acepta letras en minúscula o mayúscula.
     * Devuelve null al terminar la entrada, para que los menús salgan limpiamente.
     */
    public String leerOpcion(String indicacion, String... opciones) {
        if (opciones == null || opciones.length == 0) {
            throw new IllegalArgumentException("Debe existir al menos una opción permitida");
        }
        Set<String> permitidas = Arrays.stream(opciones)
                .map(opcion -> opcion.toUpperCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());

        while (true) {
            String valor = leerTexto(indicacion);
            if (valor == null) {
                return null;
            }
            String normalizado = valor.toUpperCase(Locale.ROOT);
            if (permitidas.contains(normalizado)) {
                return normalizado;
            }
            mostrar("[!] Opción no válida. Opciones: " + String.join(", ", opciones) + ".");
        }
    }

    /** Nunca confirma por defecto: ENTER, N o EOF cancelan. */
    public boolean confirmar(String pregunta) {
        mostrar(pregunta);
        while (true) {
            String respuesta = leerTexto("[S] Confirmar / [N o ENTER] Cancelar");
            if (respuesta == null || respuesta.isBlank() || "N".equalsIgnoreCase(respuesta)) return false;
            if ("S".equalsIgnoreCase(respuesta)) return true;
            mostrar("[!] Responde S para confirmar o N para cancelar.");
        }
    }

    /** Conserva la pantalla del resultado hasta que el usuario decida regresar. */
    public void esperar() {
        leerTexto("Presiona ENTER para regresar");
    }

}
