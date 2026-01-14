package com.reservas.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.reservas.dto.response.ReporteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Genera un reporte en PDF
     */
    public byte[] generarReportePdf(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte PDF para negocio: {}", nombreNegocio);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Título
            Paragraph title = new Paragraph("REPORTE DE NEGOCIO")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Nombre del negocio
            Paragraph businessName = new Paragraph(nombreNegocio)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(businessName);

            // Espacio
            document.add(new Paragraph("\n"));

            // Información del periodo
            Paragraph periodo = new Paragraph("Periodo: " + reporte.getPeriodo())
                    .setFontSize(12)
                    .setBold();
            document.add(periodo);

            String fechas = "";
            if (reporte.getFechaInicio() != null && reporte.getFechaFin() != null) {
                fechas = String.format("Desde: %s - Hasta: %s",
                        reporte.getFechaInicio().format(DATE_FORMATTER),
                        reporte.getFechaFin().format(DATE_FORMATTER));
            }
            document.add(new Paragraph(fechas).setFontSize(10));

            document.add(new Paragraph("\n"));

            // Tabla de resumen
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            // Encabezados
            table.addHeaderCell(new Cell()
                    .add(new Paragraph("Concepto").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
            table.addHeaderCell(new Cell()
                    .add(new Paragraph("Valor").setBold())
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));

            // Datos
            table.addCell("Total de Citas");
            table.addCell(String.valueOf(reporte.getTotalCitas()));

            table.addCell("Citas Completadas");
            table.addCell(String.valueOf(reporte.getCitasCompletadas()));

            table.addCell("Citas Canceladas");
            table.addCell(String.valueOf(reporte.getCitasCanceladas()));

            table.addCell("Citas Pendientes");
            table.addCell(String.valueOf(reporte.getCitasPendientes()));

            table.addCell("Ingreso Total");
            table.addCell(String.format("$%.2f MXN", reporte.getIngresoTotal()));

            if (reporte.getIngresoEstimado() != null && reporte.getIngresoEstimado().compareTo(java.math.BigDecimal.ZERO) > 0) {
                table.addCell("Ingreso Estimado");
                table.addCell(String.format("$%.2f MXN", reporte.getIngresoEstimado()));
            }

            table.addCell("Clientes Totales");
            table.addCell(String.valueOf(reporte.getClientesTotales()));

            if (reporte.getClientesNuevos() != null) {
                table.addCell("Clientes Nuevos");
                table.addCell(String.valueOf(reporte.getClientesNuevos()));
            }

            if (reporte.getServicioMasPopular() != null) {
                table.addCell("Servicio Más Popular");
                table.addCell(reporte.getServicioMasPopular());
            }

            document.add(table);

            // Pie de página
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Generado el: " + LocalDate.now().format(DATE_FORMATTER))
                    .setFontSize(8)
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();

            log.info(" Reporte PDF generado exitosamente - {} bytes", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error(" Error al generar reporte PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte PDF: " + e.getMessage());
        }
    }

    /**
     * Genera un reporte diario en PDF
     */
    public byte[] generarReporteDiarioPdf(ReporteResponse reporte, String nombreNegocio, LocalDate fecha) {
        log.info("Generando reporte diario PDF para fecha: {}", fecha);
        return generarReportePdf(reporte, nombreNegocio);
    }

    /**
     * Genera un reporte semanal en PDF
     */
    public byte[] generarReporteSemanalPdf(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte semanal PDF");
        return generarReportePdf(reporte, nombreNegocio);
    }

    /**
     * Genera un reporte mensual en PDF
     */
    public byte[] generarReporteMensualPdf(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte mensual PDF");
        return generarReportePdf(reporte, nombreNegocio);
    }
}
