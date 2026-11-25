package com.carmotors.vista;

import java.util.Objects;

/** Punto de acceso por rol para la consola; credenciales temporales para la etapa actual. */
public final class VistaLogin {
    private final Consola consola;

    public VistaLogin(Consola consola) {
        this.consola = Objects.requireNonNull(consola);
    }

    /** Devuelve null cuando el usuario elige salir o llega EOF. */
    public SesionConsola iniciarSesion() {
        while (true) {
            consola.titulo("CARMOTORS | ACCESO");
            consola.mostrar("1. Administrador");
            consola.mostrar("2. Técnico");
            consola.mostrar("0. Salir");
            String opcion = consola.leerOpcion("Opción", "1", "2", "0");
            if (opcion == null || "0".equals(opcion)) {
                return null;
            }

            SesionConsola.Rol rol = "1".equals(opcion)
                    ? SesionConsola.Rol.ADMINISTRADOR : SesionConsola.Rol.TECNICO;
            String usuario = consola.leerTexto("Usuario");
            if (usuario == null) {
                return null;
            }
            String clave = consola.leerTexto("Contraseña");
            if (clave == null) {
                return null;
            }

            boolean acceso = rol == SesionConsola.Rol.ADMINISTRADOR
                    ? "admin".equals(usuario) && "demo123".equals(clave)
                    : ("tecnico".equals(usuario) || "empleado".equals(usuario))
                    && "demo123".equals(clave);
            if (acceso) {
                consola.mostrar("[OK] Sesión iniciada: " + rol + ".");
                return new SesionConsola(rol, usuario, null);
            }
            consola.mostrar("[!] Credenciales no válidas para el rol seleccionado. Intenta de nuevo.");
        }
    }
}
