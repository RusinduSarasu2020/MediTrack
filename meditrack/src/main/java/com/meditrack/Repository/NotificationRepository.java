package com.meditrack.repository;

import com.meditrack.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);
    List<Notification> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByPrescriptionIdOrderByCreatedAtAsc(Long prescriptionId);
    Optional<Notification> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = :now WHERE n.userId = :userId AND n.readAt IS NULL")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
