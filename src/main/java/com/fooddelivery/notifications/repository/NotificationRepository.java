package com.fooddelivery.notifications.repository;

import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB repository for Notification documents.
 */
@Repository
public interface NotificationRepository extends MongoRepository<Notification, String> {

    Optional<Notification> findByEventId(String eventId);

    Page<Notification> findByUserId(String userId, Pageable pageable);

    Page<Notification> findByUserIdAndStatusNot(String userId, NotificationStatus status, Pageable pageable);

    List<Notification> findByUserIdAndStatus(String userId, NotificationStatus status);
}
