package com.example.notificationservice.infrastructure.persistence.repository;

import com.example.notificationservice.infrastructure.persistence.entity.InAppNotificationJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * Spring Data JPA repository for InAppNotificationJpaEntity.
 */
@Repository
public interface InAppNotificationJpaRepository extends JpaRepository<InAppNotificationJpaEntity, Long> {

    Page<InAppNotificationJpaEntity> findByUserId(Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    @Modifying
    @Query("UPDATE InAppNotificationJpaEntity n SET n.isRead = true, n.readAt = :readAt WHERE n.userId = :userId AND n.isRead = false")
    void markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}
