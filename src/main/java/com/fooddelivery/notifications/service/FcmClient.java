package com.fooddelivery.notifications.service;

import org.springframework.stereotype.Component;
import java.util.logging.Logger;

@Component
public class FcmClient {

    private static final Logger logger = Logger.getLogger(FcmClient.class.getName());

    public void sendPushNotification(String userId, String eventId, String title, String body, String priority) {
        logger.info(String.format("[FCM PUSH SUCCESS] Dispatched to User: %s | EventId: %s | Priority: %s | Title: '%s' | Body: '%s'",
                userId, eventId, priority, title, body));
    }
}
