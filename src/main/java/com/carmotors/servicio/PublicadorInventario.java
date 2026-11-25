package com.carmotors.servicio;

import java.util.ArrayList;
import java.util.List;

/**
 * PUBLICADOR: LoteService y RepuestoService llaman a publicar() tras commit.
 * Esta clase avisa a los observadores registrados, por ejemplo AlertasInventarioService.
 */
public class PublicadorInventario {
    private final List<ObservadorInventario> observadores = new ArrayList<>();

    // Un servicio interesado se agrega a la lista de observadores.
    public void suscribir(ObservadorInventario observador) {
        if (observador == null) {
            throw new IllegalArgumentException("El observador es obligatorio.");
        }

        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    // Deja de avisar a un observador registrado.
    public void desuscribir(ObservadorInventario observador) {
        observadores.remove(observador);
    }

    // Los servicios llaman a este metodo DESPUES del commit.
    public void publicar(int repuestoId) {
        // Copiamos la lista para permitir que un observador se desuscriba al recibir el aviso.
        List<ObservadorInventario> copia = new ArrayList<>(observadores);

        for (ObservadorInventario observador : copia) {
            try {
                observador.inventarioCambio(repuestoId);
            } catch (RuntimeException error) {
                // La transaccion ya fue confirmada. No se puede deshacer aqui.
                // Un observador con error no debe impedir que se notifique al siguiente.
                System.err.println("No se pudo actualizar una alerta: " + error.getMessage());
            }
        }
    }
}
