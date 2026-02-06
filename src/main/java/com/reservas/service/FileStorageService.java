package com.reservas.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Servicio para almacenar archivos
 * Por ahora usa almacenamiento local, pero está preparado para migrar a Google Cloud Storage
 */
@Service
@Slf4j
public class FileStorageService {

    private final Path fileStorageLocation;

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    public FileStorageService() {
        // Directorio para almacenar logos
        this.fileStorageLocation = Paths.get("uploads/logos").toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.fileStorageLocation);
            log.info("Directorio de almacenamiento creado en: {}", this.fileStorageLocation);
        } catch (Exception ex) {
            log.error("No se pudo crear el directorio de almacenamiento", ex);
            throw new RuntimeException("No se pudo crear el directorio de almacenamiento", ex);
        }
    }

    /**
     * Almacena un archivo y retorna la URL pública
     * @param file Archivo a almacenar
     * @return URL pública del archivo
     */
    public String storeFile(MultipartFile file) {
        // Validar archivo
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());

        try {
            // Validar que el archivo no esté vacío
            if (file.isEmpty()) {
                throw new RuntimeException("El archivo está vacío");
            }

            // Validar que sea una imagen
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new RuntimeException("Solo se permiten imágenes (PNG, JPG, SVG)");
            }

            // Validar tamaño (máximo 500KB)
            if (file.getSize() > 500 * 1024) {
                throw new RuntimeException("El archivo excede el tamaño máximo de 500KB");
            }

            // Generar nombre único
            String fileExtension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFilename.substring(dotIndex);
            }
            String filename = UUID.randomUUID().toString() + fileExtension;

            // Copiar archivo al directorio de almacenamiento
            Path targetLocation = this.fileStorageLocation.resolve(filename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            log.info("✅ Archivo almacenado: {}", filename);

            // Retornar URL pública
            // En desarrollo: http://localhost:8080/api/uploads/logos/filename.png
            // En producción: Debería ser la URL de GCS o CDN
            return contextPath + "/uploads/logos/" + filename;

        } catch (IOException ex) {
            log.error("Error al almacenar archivo", ex);
            throw new RuntimeException("Error al almacenar archivo: " + ex.getMessage());
        }
    }

    /**
     * Elimina un archivo por su URL
     * @param fileUrl URL del archivo a eliminar
     */
    public void deleteFileByUrl(String fileUrl) {
        try {
            // Extraer el nombre del archivo de la URL
            String filename = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
            Path filePath = this.fileStorageLocation.resolve(filename).normalize();

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("✅ Archivo eliminado: {}", filename);
            }
        } catch (IOException ex) {
            log.error("Error al eliminar archivo", ex);
            throw new RuntimeException("Error al eliminar archivo: " + ex.getMessage());
        }
    }
}
