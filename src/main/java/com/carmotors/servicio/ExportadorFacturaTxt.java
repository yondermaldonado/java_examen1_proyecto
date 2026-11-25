package com.carmotors.servicio;

import com.carmotors.excepcion.ExportacionException;
import com.carmotors.modelo.DocumentoFactura;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/** Presenta una factura persistida como texto UTF-8. No consulta ni modifica MySQL. */
public final class ExportadorFacturaTxt {
    public String contenido(DocumentoFactura factura) {
        Objects.requireNonNull(factura);
        StringBuilder texto = new StringBuilder("CARMOTORS - FACTURA #").append(factura.id())
                .append("\nOrden de servicio: #").append(factura.ordenId())
                .append("\nFecha: ").append(factura.fechaEmision().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .append("\nCliente: ").append(linea(factura.cliente()))
                .append("\nDocumento: ").append(linea(factura.documento()))
                .append("\nTeléfono: ").append(linea(factura.telefono()))
                .append("\nCorreo: ").append(linea(factura.email()))
                .append("\nVehículo: ").append(linea(factura.marca())).append(" ").append(linea(factura.modelo()))
                .append(" | Placa: ").append(linea(factura.placa()))
                .append("\n\nREPUESTOS UTILIZADOS\n");
        if (factura.detalle().repuestos().isEmpty()) texto.append("Sin repuestos.\n");
        factura.detalle().repuestos().forEach(r -> texto.append("#").append(r.repuestoId()).append(" ")
                .append(linea(r.nombre())).append(" | cantidad: ").append(r.cantidad())
                .append(" | precio unitario: ").append(r.precioUnitarioHistorico().toPlainString())
                .append(" | importe: ").append(r.importe().toPlainString()).append("\n"));
        texto.append("\nMANO DE OBRA\n");
        if (factura.detalle().manoObra().isEmpty()) texto.append("Sin operaciones cobradas.\n");
        factura.detalle().manoObra().forEach(o -> texto.append("#").append(o.servicioCatalogoId())
                .append(" ").append(linea(o.nombre())).append(" | importe: ").append(o.precioCobrado().toPlainString()).append("\n"));
        return texto.append("\nTotal repuestos: ").append(factura.totalRepuestos().toPlainString())
                .append("\nTotal mano de obra: ").append(factura.totalManoObra().toPlainString())
                .append("\nSubtotal: ").append(factura.totalRepuestos().add(factura.totalManoObra()).toPlainString())
                .append("\nImpuestos: ").append(factura.impuestos().toPlainString())
                .append("\nTOTAL: ").append(factura.totalFinal().toPlainString()).append("\n").toString();
    }

    public Path exportar(DocumentoFactura factura, Path directorio) {
        Path temporal = null;
        try {
            Path carpeta = directorio.toAbsolutePath().normalize();
            Files.createDirectories(carpeta);
            Path destino = carpeta.resolve("factura-" + factura.id() + "-orden-" + factura.ordenId() + ".txt");
            // El archivo final solo aparece después de escribir el documento completo.
            temporal = Files.createTempFile(carpeta, ".factura-", ".tmp");
            Files.writeString(temporal, contenido(factura), StandardCharsets.UTF_8);
            Files.move(temporal, destino);
            return destino;
        } catch (IOException | SecurityException e) {
            throw new ExportacionException("No se pudo guardar el TXT; verifica la carpeta y que el archivo no exista. La factura sigue registrada.", e);
        } finally {
            if (temporal != null) {
                try { Files.deleteIfExists(temporal); } catch (IOException | SecurityException ignored) { }
            }
        }
    }

    private String linea(String texto) { return texto == null ? "No registrado" : texto.replace('\r', ' ').replace('\n', ' '); }
}
