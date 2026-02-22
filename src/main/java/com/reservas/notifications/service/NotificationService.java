package com.reservas.notifications.service;

import com.reservas.entity.NotificationLog;
import com.reservas.entity.Usuario;
import com.reservas.exception.NotificationException;
import com.reservas.notifications.domain.NotificationChannel;
import com.reservas.notifications.domain.NotificationResult;
import com.reservas.notifications.dto.SendNotificationRequest;
import com.reservas.notifications.provider.NotificationProvider;
import com.reservas.repository.NotificationLogRepository;
import com.reservas.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de negocio para gestionar notificaciones.
 */
@Slf4j
@Service
public class NotificationService {

    private final NotificationProvider whatsappProvider;
    private final NotificationProvider emailProvider;
    private final NotificationLogRepository notificationLogRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificationService(
            @Qualifier("twilioWhatsAppProvider") NotificationProvider whatsappProvider,
            @Qualifier("resendEmailProvider") NotificationProvider emailProvider,
            NotificationLogRepository notificationLogRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.whatsappProvider = whatsappProvider;
        this.emailProvider = emailProvider;
        this.notificationLogRepository = notificationLogRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Envía una notificación por WhatsApp.
     */
    @Transactional
    public NotificationLog sendWhatsApp(SendNotificationRequest request) {
        log.info("Enviando WhatsApp a: {}", request.getRecipient());

        if (!whatsappProvider.isConfigured()) {
            throw new NotificationException("WhatsApp provider no está configurado", "PROVIDER_NOT_CONFIGURED");
        }

        NotificationResult result = whatsappProvider.send(request);

        return saveNotificationLog(request, result, NotificationLog.NotificationProvider.TWILIO);
    }

    /**
     * Envía una notificación por Email.
     */
    @Transactional
    public NotificationLog sendEmail(SendNotificationRequest request) {
        log.info("Enviando email a: {}", request.getRecipient());

        if (!emailProvider.isConfigured()) {
            throw new NotificationException("Email provider no está configurado", "PROVIDER_NOT_CONFIGURED");
        }

        NotificationResult result = emailProvider.send(request);

        return saveNotificationLog(request, result, NotificationLog.NotificationProvider.RESEND);
    }

    /**
     * Envía una notificación al canal especificado.
     */
    @Transactional
    public NotificationLog send(SendNotificationRequest request) {
        return switch (request.getChannel()) {
            case WHATSAPP -> sendWhatsApp(request);
            case EMAIL -> sendEmail(request);
            default -> throw new NotificationException(
                    "Canal no soportado: " + request.getChannel(),
                    "UNSUPPORTED_CHANNEL"
            );
        };
    }

    /**
     * Envía múltiples notificaciones en lote.
     */
    @Transactional
    public List<NotificationLog> sendBatch(List<SendNotificationRequest> requests) {
        log.info("Enviando batch de {} notificaciones", requests.size());

        return requests.stream()
                .map(this::send)
                .toList();
    }

    /**
     * Obtiene el historial de notificaciones de un usuario.
     */
    public Page<NotificationLog> getNotificationsByUsuario(String usuarioId, Pageable pageable) {
        return notificationLogRepository.findByUsuarioId(UUID.fromString(usuarioId), pageable);
    }

    /**
     * Obtiene una notificación por ID.
     */
    public NotificationLog getNotificationById(String notificationId) {
        return notificationLogRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException("Notificación no encontrada", "NOTIFICATION_NOT_FOUND"));
    }

    /**
     * Actualiza el estado de una notificación (llamado desde webhooks).
     */
    @Transactional
    public void updateNotificationStatus(String providerId, NotificationLog.NotificationStatus status) {
        log.info("Actualizando estado de notificación: {} -> {}", providerId, status);

        NotificationLog log = notificationLogRepository.findByProviderMessageId(providerId)
                .orElse(null);

        if (log != null) {
            log.setStatus(status);

            switch (status) {
                case DELIVERED -> log.setDeliveredAt(LocalDateTime.now());
                case READ -> log.setReadAt(LocalDateTime.now());
                case CLICKED -> log.setClickedAt(LocalDateTime.now());
                case BOUNCED -> log.setBouncedAt(LocalDateTime.now());
            }

            notificationLogRepository.save(log);
        }
    }

    private NotificationLog saveNotificationLog(
            SendNotificationRequest request,
            NotificationResult result,
            NotificationLog.NotificationProvider provider
    ) {
        Usuario usuario = null;
        if (request.getUsuarioId() != null) {
            usuario = usuarioRepository.findById(UUID.fromString(request.getUsuarioId())).orElse(null);
        }

        NotificationLog.NotificationStatus status = result.getSuccess()
                ? NotificationLog.NotificationStatus.SENT
                : NotificationLog.NotificationStatus.FAILED;

        NotificationLog log = NotificationLog.builder()
                .usuario(usuario)
                .channel(request.getChannel())
                .provider(provider)
                .providerMessageId(result.getProviderId())
                .recipient(request.getRecipient())
                .recipientName(request.getRecipientName())
                .subject(request.getSubject())
                .content(request.getContent())
                .templateId(request.getTemplateId())
                .status(status)
                .relatedEntityId(request.getRelatedEntityId())
                .relatedEntityType(request.getRelatedEntityType())
                .sentSuccessfully(result.getSuccess())
                .errorMessage(result.getErrorMessage())
                .errorCode(result.getErrorCode())
                .retryCount(0)
                .sentAt(result.getSentAt())
                .build();

        NotificationLog saved = notificationLogRepository.save(log);
        this.log.info("Notificación registrada: {} - Status: {}", saved.getId(), status);

        return saved;
    }
}
