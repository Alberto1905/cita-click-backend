package com.reservas.exception;

import com.reservas.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  /**
   * Maneja errores de validación de @Valid en los RequestBody
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse> handleValidationExceptions(
          MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();

    ex.getBindingResult().getAllErrors().forEach((error) -> {
      String fieldName = ((FieldError) error).getField();
      String errorMessage = error.getDefaultMessage();
      errors.put(fieldName, errorMessage);
    });

    // Crear mensaje amigable con todos los errores
    StringBuilder mensaje = new StringBuilder("Errores de validación: ");
    errors.forEach((field, error) -> mensaje.append(field).append(": ").append(error).append("; "));

    log.error("MethodArgumentNotValidException - Errores de validación: {}", errors);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(mensaje.toString())
                    .data(errors)
                    .build());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse> handleUnauthorizedException(
          UnauthorizedException ex, WebRequest request) {
    log.error("UnauthorizedException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse> handleResourceNotFoundException(
          ResourceNotFoundException ex, WebRequest request) {
    log.error("ResourceNotFoundException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<ApiResponse> handleNotFoundException(
          NotFoundException ex, WebRequest request) {
    log.error("NotFoundException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiResponse> handleBadRequestException(
          BadRequestException ex, WebRequest request) {
    log.error("BadRequestException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(LimiteExcedidoException.class)
  public ResponseEntity<ApiResponse> handleLimiteExcedidoException(
          LimiteExcedidoException ex, WebRequest request) {
    log.error("LimiteExcedidoException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(PermisoInsuficienteException.class)
  public ResponseEntity<ApiResponse> handlePermisoInsuficienteException(
          PermisoInsuficienteException ex, WebRequest request) {
    log.error("PermisoInsuficienteException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse> handleIllegalArgumentException(
          IllegalArgumentException ex, WebRequest request) {
    log.error("IllegalArgumentException: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(ex.getMessage())
                    .build());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse> handleGlobalException(
          Exception ex, WebRequest request) {
    log.error("Error no manejado: ", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.builder()
                    .success(false)
                    .message("Error interno del servidor: " + ex.getMessage())
                    .build());
  }

  /**
   * Maneja errores de método HTTP no soportado (405 Method Not Allowed)
   */
  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse> handleMethodNotSupported(
          org.springframework.web.HttpRequestMethodNotSupportedException ex) {
    String mensaje = String.format("Método HTTP '%s' no soportado para esta ruta. Métodos permitidos: %s",
            ex.getMethod(),
            String.join(", ", ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[]{}));

    log.error("HttpRequestMethodNotSupportedException: {}", mensaje);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.builder()
                    .success(false)
                    .message(mensaje)
                    .build());
  }
}
