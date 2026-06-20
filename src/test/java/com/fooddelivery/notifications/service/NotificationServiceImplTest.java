package com.fooddelivery.notifications.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.notifications.dto.NotificationResponse;
import com.fooddelivery.notifications.entity.Notification;
import com.fooddelivery.notifications.entity.NotificationStatus;
import com.fooddelivery.notifications.entity.NotificationType;
import com.fooddelivery.notifications.mapper.NotificationMapper;
import com.fooddelivery.notifications.repository.NotificationRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceImplTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationMapper mapper;

    @Mock
    private FcmClient fcmClient;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user_123");
        user.setEmail("test@food.com");

        notification = new Notification("user_123", "event_123", NotificationType.ORDER, "Title", "Body", "HIGH");
    }

    @Test
    void testSendNotification_SucceedsEvenIfFcmThrowsException() {
        when(repository.findByEventId("event_123")).thenReturn(Optional.empty());
        when(userRepository.findById("user_123")).thenReturn(Optional.of(user));
        when(repository.save(any(Notification.class))).thenReturn(notification);
        when(mapper.toResponse(any(Notification.class))).thenReturn(new NotificationResponse());

        // Simulate FCM client throwing an exception
        doThrow(new RuntimeException("FCM connectivity failure"))
                .when(fcmClient).sendPushNotification(anyString(), anyString(), anyString(), anyString(), anyString());

        // This should run without throwing any exception because of the isolated try-catch block!
        assertDoesNotThrow(() -> {
            notificationService.sendNotification("user_123", "event_123", NotificationType.ORDER, "Title", "Body", "HIGH");
        });

        // Verify notification is still saved in the database
        verify(repository, times(1)).save(any(Notification.class));
        // Verify WebSocket was still invoked
        verify(simpMessagingTemplate, times(1)).convertAndSendToUser(eq("test@food.com"), eq("/queue/notifications"), any());
    }
}
