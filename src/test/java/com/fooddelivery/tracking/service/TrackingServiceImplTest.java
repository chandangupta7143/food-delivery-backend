package com.fooddelivery.tracking.service;

import com.fooddelivery.common.enums.Role;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.orders.entity.EventType;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.entity.StatusEvent;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.tracking.dto.TimelineEventResponse;
import com.fooddelivery.tracking.entity.DriverLocationHistory;
import com.fooddelivery.tracking.repository.DriverLocationHistoryRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TrackingServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DeliveryPartnerRepository partnerRepository;

    @Mock
    private DriverLocationHistoryRepository historyRepository;

    @InjectMocks
    private TrackingServiceImpl trackingService;

    private User customer;
    private User driverUser;
    private User admin;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId("user_customer");
        customer.setEmail("customer@food.com");
        customer.setRole(Role.USER);

        driverUser = new User();
        driverUser.setId("user_driver");
        driverUser.setEmail("driver@food.com");
        driverUser.setRole(Role.DELIVERY_PARTNER);

        admin = new User();
        admin.setId("user_admin");
        admin.setEmail("admin@food.com");
        admin.setRole(Role.ADMIN);

        order = new Order();
        order.setId("order_123");
        order.setUserId("user_customer");
        order.setDeliveryPartnerId("user_driver");
        order.setStatus(OrderStatus.CREATED);
        
        List<StatusEvent> statusHistory = new ArrayList<>();
        statusHistory.add(new StatusEvent(OrderStatus.CREATED, EventType.SYSTEM_EVENT, LocalDateTime.now(), "SYSTEM", "SYSTEM"));
        statusHistory.add(new StatusEvent(OrderStatus.PENDING_REVIEW, EventType.SYSTEM_EVENT, LocalDateTime.now(), "SYSTEM", "SYSTEM"));
        order.setStatusHistory(statusHistory);
    }

    @Test
    void testGetVisibleTimeline_AsCustomer_MasksPendingReview() {
        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@food.com")).thenReturn(Optional.of(customer));

        List<TimelineEventResponse> timeline = trackingService.getVisibleTimeline("order_123", "customer@food.com");

        // The timeline has two events: CREATED and PENDING_REVIEW.
        // PENDING_REVIEW should be masked/filtered out for customers.
        assertEquals(1, timeline.size());
        assertEquals(OrderStatus.CREATED, timeline.get(0).getStatus());
    }

    @Test
    void testGetVisibleTimeline_AsAdmin_SeesAllEvents() {
        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("admin@food.com")).thenReturn(Optional.of(admin));

        List<TimelineEventResponse> timeline = trackingService.getVisibleTimeline("order_123", "admin@food.com");

        // Admin should see both CREATED and PENDING_REVIEW
        assertEquals(2, timeline.size());
    }

    @Test
    void testGetFilteredDriverLocation_LockedBeforeOutForDelivery() {
        order.setStatus(OrderStatus.PREPARING);

        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@food.com")).thenReturn(Optional.of(customer));

        GeoJsonPoint loc = trackingService.getFilteredDriverLocation("order_123", "customer@food.com");

        // Should return null coordinate frame because order is not OUT_FOR_DELIVERY
        assertNull(loc);
    }

    @Test
    void testGetFilteredDriverLocation_VisibleAfterOutForDelivery() {
        order.setStatus(OrderStatus.OUT_FOR_DELIVERY);
        DeliveryPartner partner = new DeliveryPartner();
        partner.setId("user_driver");
        GeoJsonPoint driverLoc = new GeoJsonPoint(77.5946, 12.9716);
        partner.setCurrentLocation(driverLoc);

        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("customer@food.com")).thenReturn(Optional.of(customer));
        when(partnerRepository.findById("user_driver")).thenReturn(Optional.of(partner));

        GeoJsonPoint loc = trackingService.getFilteredDriverLocation("order_123", "customer@food.com");

        // Should return actual coordinates
        assertNotNull(loc);
        assertEquals(77.5946, loc.getX());
        assertEquals(12.9716, loc.getY());
    }

    @Test
    void testGetFilteredDriverLocation_AccessDeniedForUnauthorizedUser() {
        User otherUser = new User();
        otherUser.setId("other_user");
        otherUser.setEmail("other@food.com");
        otherUser.setRole(Role.USER);

        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("other@food.com")).thenReturn(Optional.of(otherUser));

        assertThrows(SecurityException.class, () -> {
            trackingService.getFilteredDriverLocation("order_123", "other@food.com");
        });
    }
}
