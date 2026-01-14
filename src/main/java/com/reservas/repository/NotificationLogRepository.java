package com.reservas.repository;

import com.reservas.entity.NotificationLog;
import com.reservas.notifications.domain.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, String> {

    Optional<NotificationLog> findByProviderMessageId(String providerMessageId);

    List<NotificationLog> findByUsuarioIdOrderByCreatedAtDesc(UUID usuarioId);

    Page<NotificationLog> findByUsuarioId(UUID usuarioId, Pageable pageable);

    List<NotificationLog> findByChannelAndStatus(NotificationChannel channel, NotificationLog.NotificationStatus status);

    List<NotificationLog> findByRelatedEntityIdAndRelatedEntityType(String relatedEntityId, String relatedEntityType);

    List<NotificationLog> findByRecipientAndCreatedAtAfter(String recipient, LocalDateTime date);

    Long countByUsuarioIdAndStatus(UUID usuarioId, NotificationLog.NotificationStatus status);
}
