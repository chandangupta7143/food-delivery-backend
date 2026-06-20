package com.fooddelivery.notifications.service;

import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.NotificationType;

public interface NotificationService {

    void sendNotification(String userId, String eventId, NotificationType type, String title, String body, String priority);

    PaginatedResponse<NotificationResponse> getUserNotifications(String email, int page, int size);

    NotificationResponse markAsRead(String id, String email);

    void markAllAsRead(String email);

    void archiveNotification(String id, String email);
}
