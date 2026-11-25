package com.carmotors.vista;

import java.util.Objects;

/** Acceso a los CRUD reales ya conectados. */
public final class MenuGestionAdministrativa {
    private final Consola consola;
    private final VistaClientes clientes;
    private final VistaVehiculos vehiculos;
    private final VistaTecnicos tecnicos;
    private final VistaCatalogoRepuestos catalogo;
    private final VistaProveedores proveedores;

    public MenuGestionAdministrativa(Consola consola, VistaClientes clientes,
                                     VistaVehiculos vehiculos, VistaTecnicos tecnicos,
                                     VistaCatalogoRepuestos catalogo,
                                     VistaProveedores proveedores) {
        this.consola = Objects.requireNonNull(consola);
        this.clientes = Objects.requireNonNull(clientes);
        this.vehiculos = Objects.requireNonNull(vehiculos);
        this.tecnicos = Objects.requireNonNull(tecnicos);
        this.catalogo = Objects.requireNonNull(catalogo);
        this.proveedores = Objects.requireNonNull(proveedores);
    }

    public void mostrar(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.ADMINISTRADOR) {
            throw new IllegalArgumentException("Se requiere rol administrativo");
        }
        while (true) {
            consola.titulo("GESTIÓN ADMINISTRATIVA");
            consola.mostrar("1. Gestión de clientes");
            consola.mostrar("2. Gestión de vehículos");
            consola.mostrar("3. Gestión de técnicos");
            consola.mostrar("4. Catálogo de repuestos y tipos");
            consola.mostrar("5. Proveedores, ofertas y evaluaciones");
            consola.mostrar("0. Regresar");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> clientes.mostrarGestion();
                case "2" -> vehiculos.mostrarGestion();
                case "3" -> tecnicos.mostrarGestion();
                case "4" -> catalogo.mostrarGestion();
                case "5" -> proveedores.mostrarGestion();
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }
}
