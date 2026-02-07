package com.reservas.service;

import com.reservas.dto.response.ReporteResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para PdfService
 */
@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @InjectMocks
    private PdfService pdfService;

    @Test
    void testGenerarReportePdf_ConDatosCompletos() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("MENSUAL")
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 31))
                .totalCitas(50)
                .citasCompletadas(40)
                .citasCanceladas(5)
                .citasPendientes(5)
                .ingresoTotal(BigDecimal.valueOf(15000.00))
                .ingresoEstimado(BigDecimal.valueOf(1500.00))
                .clientesTotales(35)
                .clientesNuevos(10)
                .servicioMasPopular("Corte de Cabello")
                .build();

        String nombreNegocio = "Barbería El Clásico";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Verificar que es un PDF válido (comienza con %PDF)
        assertEquals('%', pdfBytes[0]);
        assertEquals('P', pdfBytes[1]);
        assertEquals('D', pdfBytes[2]);
        assertEquals('F', pdfBytes[3]);
    }

    @Test
    void testGenerarReportePdf_ConDatosMinimos() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("DIARIO")
                .totalCitas(10)
                .citasCompletadas(8)
                .citasCanceladas(2)
                .citasPendientes(0)
                .ingresoTotal(BigDecimal.valueOf(2000.00))
                .clientesTotales(8)
                .build();

        String nombreNegocio = "Spa Bienestar";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReportePdf_ConIngresoEstimadoCero() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("SEMANAL")
                .fechaInicio(LocalDate.of(2026, 1, 6))
                .fechaFin(LocalDate.of(2026, 1, 12))
                .totalCitas(20)
                .citasCompletadas(15)
                .citasCanceladas(3)
                .citasPendientes(2)
                .ingresoTotal(BigDecimal.valueOf(5000.00))
                .ingresoEstimado(BigDecimal.ZERO)
                .clientesTotales(15)
                .clientesNuevos(5)
                .build();

        String nombreNegocio = "Salón Belleza Total";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReportePdf_SinFechas() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("PERSONALIZADO")
                .totalCitas(15)
                .citasCompletadas(12)
                .citasCanceladas(2)
                .citasPendientes(1)
                .ingresoTotal(BigDecimal.valueOf(3500.00))
                .clientesTotales(12)
                .build();

        String nombreNegocio = "Consultorio Dental";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReportePdf_ConServicioMasPopular() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("MENSUAL")
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 31))
                .totalCitas(100)
                .citasCompletadas(90)
                .citasCanceladas(5)
                .citasPendientes(5)
                .ingresoTotal(BigDecimal.valueOf(25000.00))
                .ingresoEstimado(BigDecimal.valueOf(2500.00))
                .clientesTotales(70)
                .clientesNuevos(20)
                .servicioMasPopular("Masaje Relajante")
                .servicioMasPopularCantidad(35)
                .build();

        String nombreNegocio = "Centro de Masajes Zen";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReportePdf_LanzaExcepcionConReporteNull() {
        // Given
        ReporteResponse reporte = null;
        String nombreNegocio = "Negocio Test";

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            pdfService.generarReportePdf(reporte, nombreNegocio);
        });
    }

    @Test
    void testGenerarReporteDiarioPdf_Exitoso() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("DIARIO")
                .fechaInicio(LocalDate.of(2026, 1, 15))
                .fechaFin(LocalDate.of(2026, 1, 15))
                .totalCitas(8)
                .citasCompletadas(6)
                .citasCanceladas(1)
                .citasPendientes(1)
                .ingresoTotal(BigDecimal.valueOf(1800.00))
                .clientesTotales(6)
                .clientesNuevos(2)
                .build();

        String nombreNegocio = "Clínica Dental";
        LocalDate fecha = LocalDate.of(2026, 1, 15);

        // When
        byte[] pdfBytes = pdfService.generarReporteDiarioPdf(reporte, nombreNegocio, fecha);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReporteSemanalPdf_Exitoso() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("SEMANAL")
                .fechaInicio(LocalDate.of(2026, 1, 13))
                .fechaFin(LocalDate.of(2026, 1, 19))
                .totalCitas(35)
                .citasCompletadas(30)
                .citasCanceladas(3)
                .citasPendientes(2)
                .ingresoTotal(BigDecimal.valueOf(7500.00))
                .clientesTotales(28)
                .clientesNuevos(8)
                .build();

        String nombreNegocio = "Gimnasio PowerFit";

        // When
        byte[] pdfBytes = pdfService.generarReporteSemanalPdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReporteMensualPdf_Exitoso() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("MENSUAL")
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 31))
                .totalCitas(120)
                .citasCompletadas(100)
                .citasCanceladas(10)
                .citasPendientes(10)
                .ingresoTotal(BigDecimal.valueOf(30000.00))
                .ingresoEstimado(BigDecimal.valueOf(3000.00))
                .clientesTotales(85)
                .clientesNuevos(25)
                .servicioMasPopular("Consulta General")
                .build();

        String nombreNegocio = "Consultorio Médico";

        // When
        byte[] pdfBytes = pdfService.generarReporteMensualPdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }

    @Test
    void testGenerarReportePdf_ConIngresosGrandes() {
        // Given
        ReporteResponse reporte = ReporteResponse.builder()
                .periodo("MENSUAL")
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 1, 31))
                .totalCitas(500)
                .citasCompletadas(450)
                .citasCanceladas(25)
                .citasPendientes(25)
                .ingresoTotal(BigDecimal.valueOf(125000.50))
                .ingresoEstimado(BigDecimal.valueOf(12500.75))
                .clientesTotales(350)
                .clientesNuevos(100)
                .servicioMasPopular("Tratamiento Premium")
                .build();

        String nombreNegocio = "Spa Luxury Resort";

        // When
        byte[] pdfBytes = pdfService.generarReportePdf(reporte, nombreNegocio);

        // Then
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
