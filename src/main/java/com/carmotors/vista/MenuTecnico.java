package com.carmotors.vista;

import java.util.Objects;

/** Navegación técnica con un técnico seleccionado y trabajos filtrados por su ID. */
public final class MenuTecnico {
    private final Consola consola;
    private final VistaClientes clientes;
    private final VistaVehiculos vehiculos;
    private final VistaOrdenes ordenes;
    private final VistaTrabajosTecnico trabajos;
    private final VistaInventario inventario;
    private final VistaHistorial historial;

    public MenuTecnico(Consola consola, VistaClientes clientes,
                       VistaVehiculos vehiculos, VistaOrdenes ordenes,
                       VistaTrabajosTecnico trabajos, VistaInventario inventario,
                       VistaHistorial historial) {
        this.consola = Objects.requireNonNull(consola);
        this.clientes = Objects.requireNonNull(clientes);
        this.vehiculos = Objects.requireNonNull(vehiculos);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.trabajos = Objects.requireNonNull(trabajos);
        this.inventario = Objects.requireNonNull(inventario);
        this.historial = Objects.requireNonNull(historial);
    }

    public void mostrar(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.TECNICO) {
            throw new IllegalArgumentException("Se requiere rol técnico");
        }
        SesionConsola actual = trabajos.seleccionarTecnico(sesion);
        if (actual == null) return;
        while (true) {
            consola.titulo("CARMOTORS | TÉCNICO");
            consola.mostrar("Técnico seleccionado: "
                    + (actual.tecnicoId() == null ? "Ninguno" : "#" + actual.tecnicoId()));
            consola.mostrar("1. Mis trabajos (activos e historial)");
            consola.mostrar("2. Registrar / consultar clientes");
            consola.mostrar("3. Consultar vehículos");
            consola.mostrar("4. Crear / consultar órdenes de servicio");
            consola.mostrar("5. Cambiar técnico seleccionado");
            consola.mostrar("6. Inventario y recepción de lotes");
            consola.mostrar("7. Historial y recordatorios preventivos");
            consola.mostrar("0. Cerrar sesión");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "7", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> trabajos.mostrar(actual);
                case "2" -> clientes.mostrarParaTecnico();
                case "3" -> vehiculos.mostrarParaTecnico();
                case "4" -> ordenes.mostrarParaTecnico();
                case "6" -> inventario.mostrarParaTecnico();
                case "7" -> historial.mostrar();
                case "5" -> {
                    SesionConsola nueva = trabajos.seleccionarTecnico(actual);
                    if (nueva == null) return;
                    actual = nueva;
                }
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }
}
