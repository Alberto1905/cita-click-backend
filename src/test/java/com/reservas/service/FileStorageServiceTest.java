package com.reservas.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para FileStorageService
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @InjectMocks
    private FileStorageService fileStorageService;

    private Path testFileStorageLocation;

    @BeforeEach
    void setUp() throws IOException {
        // Crear directorio temporal para tests
        testFileStorageLocation = Paths.get("uploads/logos-test").toAbsolutePath().normalize();
        Files.createDirectories(testFileStorageLocation);

        // Configurar el servicio para usar el directorio de test
        ReflectionTestUtils.setField(fileStorageService, "fileStorageLocation", testFileStorageLocation);
        ReflectionTestUtils.setField(fileStorageService, "contextPath", "/api");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Limpiar archivos de test
        if (Files.exists(testFileStorageLocation)) {
            Files.walk(testFileStorageLocation)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            // Ignorar errores de limpieza
                        }
                    });
        }
    }

    @Test
    void testStoreFile_ImagenPngExitosa() {
        // Given
        byte[] content = "fake image content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                content
        );

        // When
        String fileUrl = fileStorageService.storeFile(file);

        // Then
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/uploads/logos/"));
        assertTrue(fileUrl.endsWith(".png"));
    }

    @Test
    void testStoreFile_ImagenJpgExitosa() {
        // Given
        byte[] content = "fake jpeg content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                content
        );

        // When
        String fileUrl = fileStorageService.storeFile(file);

        // Then
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/uploads/logos/"));
        assertTrue(fileUrl.endsWith(".jpg"));
    }

    @Test
    void testStoreFile_ImagenSvgExitosa() {
        // Given
        byte[] content = "<svg>content</svg>".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "icon.svg",
                "image/svg+xml",
                content
        );

        // When
        String fileUrl = fileStorageService.storeFile(file);

        // Then
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/uploads/logos/"));
        assertTrue(fileUrl.endsWith(".svg"));
    }

    @Test
    void testStoreFile_ArchivoVacio_LanzaExcepcion() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(file);
        });
        assertEquals("El archivo está vacío", exception.getMessage());
    }

    @Test
    void testStoreFile_ArchivoNoEsImagen_LanzaExcepcion() {
        // Given
        byte[] content = "not an image".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                content
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(file);
        });
        assertEquals("Solo se permiten imágenes (PNG, JPG, SVG)", exception.getMessage());
    }

    @Test
    void testStoreFile_ContentTypeNull_LanzaExcepcion() {
        // Given
        byte[] content = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "file.png",
                null,
                content
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(file);
        });
        assertEquals("Solo se permiten imágenes (PNG, JPG, SVG)", exception.getMessage());
    }

    @Test
    void testStoreFile_ArchivoExcedeTamanoMaximo_LanzaExcepcion() {
        // Given - Crear archivo de 600KB (excede límite de 500KB)
        byte[] content = new byte[600 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large-image.png",
                "image/png",
                content
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            fileStorageService.storeFile(file);
        });
        assertEquals("El archivo excede el tamaño máximo de 500KB", exception.getMessage());
    }

    @Test
    void testStoreFile_ArchivoEnLimiteDeTamano_Exitoso() {
        // Given - Crear archivo de exactamente 500KB
        byte[] content = new byte[500 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "max-size.png",
                "image/png",
                content
        );

        // When
        String fileUrl = fileStorageService.storeFile(file);

        // Then
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/uploads/logos/"));
    }

    @Test
    void testStoreFile_NombreArchivoConEspacios() {
        // Given
        byte[] content = "content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mi logo empresarial.png",
                "image/png",
                content
        );

        // When
        String fileUrl = fileStorageService.storeFile(file);

        // Then
        assertNotNull(fileUrl);
        assertTrue(fileUrl.startsWith("/api/uploads/logos/"));
        assertTrue(fileUrl.endsWith(".png"));
    }

    @Test
    void testDeleteFileByUrl_ArchivoExistente() throws IOException {
        // Given - Primero subir un archivo
        byte[] content = "test content".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "to-delete.png",
                "image/png",
                content
        );
        String fileUrl = fileStorageService.storeFile(file);

        // When
        assertDoesNotThrow(() -> {
            fileStorageService.deleteFileByUrl(fileUrl);
        });

        // Then - Verificar que el archivo ya no existe
        String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path filePath = testFileStorageLocation.resolve(filename);
        assertFalse(Files.exists(filePath));
    }

    @Test
    void testDeleteFileByUrl_ArchivoNoExistente_NoLanzaExcepcion() {
        // Given
        String fileUrl = "/api/uploads/logos/non-existent-file.png";

        // When & Then - No debe lanzar excepción
        assertDoesNotThrow(() -> {
            fileStorageService.deleteFileByUrl(fileUrl);
        });
    }

    @Test
    void testStoreFile_GeneraNombreUnico() {
        // Given
        byte[] content1 = "content 1".getBytes();
        byte[] content2 = "content 2".getBytes();

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                content1
        );

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                content2
        );

        // When
        String fileUrl1 = fileStorageService.storeFile(file1);
        String fileUrl2 = fileStorageService.storeFile(file2);

        // Then - Los nombres deben ser diferentes
        assertNotEquals(fileUrl1, fileUrl2);
        assertTrue(fileUrl1.endsWith(".png"));
        assertTrue(fileUrl2.endsWith(".png"));
    }
}
