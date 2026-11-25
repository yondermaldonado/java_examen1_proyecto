package com.carmotors.vista;

import java.util.Objects;

/** Navegación del administrador a módulos conectados con los servicios reales. */
public final class MenuAdministrador {
    private final Consola consola;
    private final MenuGestionAdministrativa gestion;
    private final VistaOrdenes ordenes;
    private final VistaSupervision supervision;
    private final VistaInventario inventario;
    private final VistaFacturacion facturacion;
    private final VistaHistorial historial;
    private final VistaReportes reportes;

    public MenuAdministrador(Consola consola, MenuGestionAdministrativa gestion,
                             VistaOrdenes ordenes, VistaSupervision supervision,
                             VistaInventario inventario, VistaFacturacion facturacion,
                             VistaHistorial historial, VistaReportes reportes) {
        this.consola = Objects.requireNonNull(consola);
        this.gestion = Objects.requireNonNull(gestion);
        this.ordenes = Objects.requireNonNull(ordenes);
        this.supervision = Objects.requireNonNull(supervision);
        this.inventario = Objects.requireNonNull(inventario);
        this.facturacion = Objects.requireNonNull(facturacion);
        this.historial = Objects.requireNonNull(historial);
        this.reportes = Objects.requireNonNull(reportes);
    }

    public void mostrar(SesionConsola sesion) {
        if (sesion.rol() != SesionConsola.Rol.ADMINISTRADOR) {
            throw new IllegalArgumentException("Se requiere rol administrativo");
        }
        while (true) {
            consola.titulo("CARMOTORS | ADMINISTRADOR");
            consola.mostrar("1. Supervisión del taller");
            consola.mostrar("2. Atención y órdenes de servicio");
            consola.mostrar("3. Gestión administrativa (clientes, vehículos, técnicos, repuestos y proveedores)");
            consola.mostrar("4. Inventario y abastecimiento");
            consola.mostrar("5. Facturación y entrega de vehículos");
            consola.mostrar("6. Historial y recordatorios preventivos");
            consola.mostrar("7. Reportes e indicadores");
            consola.mostrar("0. Cerrar sesión");
            String opcion = consola.leerOpcion("Opción", "1", "2", "3", "4", "5", "6", "7", "0");
            if (opcion == null || "0".equals(opcion)) return;
            switch (opcion) {
                case "1" -> supervision.mostrar(sesion);
                case "2" -> ordenes.mostrarAdministracion();
                case "3" -> gestion.mostrar(sesion);
                case "4" -> inventario.mostrarAdministracion();
                case "5" -> facturacion.mostrar(sesion);
                case "6" -> historial.mostrar();
                case "7" -> reportes.mostrar(sesion);
                default -> throw new IllegalStateException("Opción ya validada");
            }
        }
    }
}
