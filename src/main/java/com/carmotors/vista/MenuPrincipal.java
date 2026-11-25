package com.carmotors.vista;

import java.util.Objects;

/** Coordina el ingreso y el retorno al login. No construye servicios ni DAO. */
public final class MenuPrincipal {
    private final Consola consola;
    private final VistaLogin login;
    private final MenuAdministrador administrador;
    private final MenuTecnico tecnico;

    public MenuPrincipal(Consola consola, VistaLogin login,
                         MenuAdministrador administrador, MenuTecnico tecnico) {
        this.consola = Objects.requireNonNull(consola);
        this.login = Objects.requireNonNull(login);
        this.administrador = Objects.requireNonNull(administrador);
        this.tecnico = Objects.requireNonNull(tecnico);
    }

    public void iniciar() {
        while (true) {
            SesionConsola sesion = login.iniciarSesion();
            if (sesion == null) {
                consola.mostrar("Gracias por utilizar CarMotors. Hasta pronto.");
                return;
            }
            if (sesion.rol() == SesionConsola.Rol.ADMINISTRADOR) {
                administrador.mostrar(sesion);
            } else {
                tecnico.mostrar(sesion);
            }
            consola.mostrar("[OK] Sesión finalizada. Regresando al acceso.");
        }
    }
}
