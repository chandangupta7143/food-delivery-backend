package com.fooddelivery.notifications.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.entity.NotificationStatus;
import com.fooddelivery.notifications.entity.NotificationType;
import com.fooddelivery.notifications.mapper.NotificationMapper;
import com.fooddelivery.notifications.repository.NotificationRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = Logger.getLogger(NotificationServiceImpl.class.getName());

    private final NotificationRepository repository;
    private final UserRepository userRepository;
    private final NotificationMapper mapper;
    private final FcmClient fcmClient;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public NotificationServiceImpl(NotificationRepository repository,
                                   UserRepository userRepository,
                                   NotificationMapper mapper,
                                   FcmClient fcmClient,
                                   SimpMessagingTemplate simpMessagingTemplate) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.fcmClient = fcmClient;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    @Transactional
    public void sendNotification(String userId, String eventId, NotificationType type, String title, String body, String priority) {
        // Deduplication Guard
        if (eventId != null && !eventId.trim().isEmpty()) {
            Optional<Notification> existing = repository.findByEventId(eventId);
            if (existing.isPresent()) {
                logger.warning(String.format("Duplicate notification ignored. EventId: %s", eventId));
                return;
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Notification notification = new Notification(userId, eventId, type, title, body, priority);
        notification = repository.save(notification);

        // 1. WebSocket Live Dispatch
        try {
            simpMessagingTemplate.convertAndSendToUser(
                    user.getEmail(),
                    "/queue/notifications",
                    mapper.toResponse(notification)
            );
            logger.info("Real-time WebSocket notification dispatched to: " + user.getEmail());
        } catch (Exception e) {
            logger.warning("Failed to dispatch real-time WebSocket notification: " + e.getMessage());
        }

        // 2. FCM Push Dispatch (Always dispatched for tracing/auditing)
        try {
            fcmClient.sendPushNotification(userId, eventId, title, body, priority);
        } catch (Exception e) {
            logger.severe("FCM push delivery failed: " + e.getMessage());
        }
    }

    @Override
    public PaginatedResponse<NotificationResponse> getUserNotifications(String email, int page, int size) {
        User user = findUserByEmailOrThrow(email);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        // Fetch active notifications (UNREAD and READ, excluding ARCHIVED)
        Page<Notification> result = repository.findByUserIdAndStatusNot(user.getId(), NotificationStatus.ARCHIVED, pageable);

        List<NotificationResponse> content = result.getContent().stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new PaginatedResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(String id, String email) {
        User user = findUserByEmailOrThrow(email);
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to notification");
        }

        notification.setStatus(NotificationStatus.READ);
        notification = repository.save(notification);

        return mapper.toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(String email) {
        User user = findUserByEmailOrThrow(email);
        List<Notification> unread = repository.findByUserIdAndStatus(user.getId(), NotificationStatus.UNREAD);

        for (Notification notification : unread) {
            notification.setStatus(NotificationStatus.READ);
        }
        repository.saveAll(unread);
    }

    @Override
    @Transactional
    public void archiveNotification(String id, String email) {
        User user = findUserByEmailOrThrow(email);
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!notification.getUserId().equals(user.getId())) {
            throw new SecurityException("Unauthorized access to notification");
        }

        notification.setStatus(NotificationStatus.ARCHIVED);
        repository.save(notification);
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }
}
