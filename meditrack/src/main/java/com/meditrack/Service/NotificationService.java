package com.meditrack.service;

import com.meditrack.enums.NotificationType;
import com.meditrack.model.Notification;
import com.meditrack.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final int MAX_MESSAGE_LENGTH = 255;

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void notify(Long userId, String message, String link) {
        repository.save(new Notification(userId, truncate(message), link));
    }

    /** Persists a typed notification, optionally tied to a prescription. Joins the caller's transaction. */
    @Transactional
    public Notification notify(Long userId, NotificationType type, String title, String message, String link, Long prescriptionId) {
        return repository.save(new Notification(userId, type, title, truncate(message), link, prescriptionId));
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getLatestForUser(Long userId) {
        return repository.findTop5ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getForPrescription(Long prescriptionId) {
        return repository.findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        repository.findByIdAndUserId(notificationId, userId).ifPresent(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(Instant.now());
                repository.save(n);
            }
        });
    }

    /** Marks the user's own notification as read and returns it; empty if it belongs to someone else. */
    @Transactional
    public Optional<Notification> openForUser(Long notificationId, Long userId) {
        Optional<Notification> notification = repository.findByIdAndUserId(notificationId, userId);
        notification.ifPresent(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(Instant.now());
                repository.save(n);
            }
        });
        return notification;
    }

    @Transactional
    public int markAllAsRead(Long userId) {
        return repository.markAllRead(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        return (int) repository.countByUserIdAndReadAtIsNull(userId);
    }

    private static String truncate(String message) {
        if (message == null) return "";
        return message.length() <= MAX_MESSAGE_LENGTH ? message : message.substring(0, MAX_MESSAGE_LENGTH - 1) + "…";
    }
}
