package com.reservas.service;

import com.reservas.dto.response.ReporteResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class ExcelService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Genera un reporte en Excel
     */
    public byte[] generarReporteExcel(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte Excel para negocio: {}", nombreNegocio);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 12);
            titleStyle.setFont(titleFont);

            CellStyle currencyStyle = workbook.createCellStyle();
            currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));

            int rowNum = 0;

            // Título
            Row titleRow = sheet.createRow(rowNum++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("REPORTE DE NEGOCIO");
            titleCell.setCellStyle(headerStyle);

            // Nombre del negocio
            Row businessRow = sheet.createRow(rowNum++);
            Cell businessCell = businessRow.createCell(0);
            businessCell.setCellValue(nombreNegocio);
            businessCell.setCellStyle(headerStyle);

            rowNum++; // Espacio

            // Periodo
            Row periodoRow = sheet.createRow(rowNum++);
            periodoRow.createCell(0).setCellValue("Periodo:");
            periodoRow.createCell(1).setCellValue(reporte.getPeriodo());

            // Fechas
            if (reporte.getFechaInicio() != null && reporte.getFechaFin() != null) {
                Row fechasRow = sheet.createRow(rowNum++);
                fechasRow.createCell(0).setCellValue("Fechas:");
                String fechas = String.format("%s - %s",
                        reporte.getFechaInicio().format(DATE_FORMATTER),
                        reporte.getFechaFin().format(DATE_FORMATTER));
                fechasRow.createCell(1).setCellValue(fechas);
            }

            rowNum++; // Espacio

            // Encabezados de tabla
            Row headerRow = sheet.createRow(rowNum++);
            Cell conceptoHeader = headerRow.createCell(0);
            conceptoHeader.setCellValue("Concepto");
            conceptoHeader.setCellStyle(titleStyle);

            Cell valorHeader = headerRow.createCell(1);
            valorHeader.setCellValue("Valor");
            valorHeader.setCellStyle(titleStyle);

            // Datos
            addDataRow(sheet, rowNum++, "Total de Citas", reporte.getTotalCitas());
            addDataRow(sheet, rowNum++, "Citas Completadas", reporte.getCitasCompletadas());
            addDataRow(sheet, rowNum++, "Citas Canceladas", reporte.getCitasCanceladas());
            addDataRow(sheet, rowNum++, "Citas Pendientes", reporte.getCitasPendientes());

            // Ingresos
            Row ingresoRow = sheet.createRow(rowNum++);
            ingresoRow.createCell(0).setCellValue("Ingreso Total");
            Cell ingresoCell = ingresoRow.createCell(1);
            ingresoCell.setCellValue(reporte.getIngresoTotal().doubleValue());
            ingresoCell.setCellStyle(currencyStyle);

            if (reporte.getIngresoEstimado() != null && reporte.getIngresoEstimado().compareTo(java.math.BigDecimal.ZERO) > 0) {
                Row estimadoRow = sheet.createRow(rowNum++);
                estimadoRow.createCell(0).setCellValue("Ingreso Estimado");
                Cell estimadoCell = estimadoRow.createCell(1);
                estimadoCell.setCellValue(reporte.getIngresoEstimado().doubleValue());
                estimadoCell.setCellStyle(currencyStyle);
            }

            addDataRow(sheet, rowNum++, "Clientes Totales", reporte.getClientesTotales());

            if (reporte.getClientesNuevos() != null) {
                addDataRow(sheet, rowNum++, "Clientes Nuevos", reporte.getClientesNuevos());
            }

            if (reporte.getServicioMasPopular() != null) {
                Row servicioRow = sheet.createRow(rowNum++);
                servicioRow.createCell(0).setCellValue("Servicio Más Popular");
                servicioRow.createCell(1).setCellValue(reporte.getServicioMasPopular());
            }

            rowNum++; // Espacio

            // Pie de página
            Row footerRow = sheet.createRow(rowNum);
            footerRow.createCell(0).setCellValue("Generado el:");
            footerRow.createCell(1).setCellValue(LocalDate.now().format(DATE_FORMATTER));

            // Ajustar ancho de columnas
            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.setColumnWidth(0, sheet.getColumnWidth(0) + 2000);
            sheet.setColumnWidth(1, sheet.getColumnWidth(1) + 2000);

            workbook.write(baos);

            log.info(" Reporte Excel generado exitosamente - {} bytes", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error(" Error al generar reporte Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Error al generar reporte Excel: " + e.getMessage());
        }
    }

    private void addDataRow(Sheet sheet, int rowNum, String concepto, int valor) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(concepto);
        row.createCell(1).setCellValue(valor);
    }

    /**
     * Genera un reporte diario en Excel
     */
    public byte[] generarReporteDiarioExcel(ReporteResponse reporte, String nombreNegocio, LocalDate fecha) {
        log.info("Generando reporte diario Excel para fecha: {}", fecha);
        return generarReporteExcel(reporte, nombreNegocio);
    }

    /**
     * Genera un reporte semanal en Excel
     */
    public byte[] generarReporteSemanalExcel(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte semanal Excel");
        return generarReporteExcel(reporte, nombreNegocio);
    }

    /**
     * Genera un reporte mensual en Excel
     */
    public byte[] generarReporteMensualExcel(ReporteResponse reporte, String nombreNegocio) {
        log.info("Generando reporte mensual Excel");
        return generarReporteExcel(reporte, nombreNegocio);
    }
}
