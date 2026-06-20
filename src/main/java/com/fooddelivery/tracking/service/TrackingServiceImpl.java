package com.fooddelivery.tracking.service;

import com.fooddelivery.common.enums.Role;
import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.OrderStatus;
import com.fooddelivery.orders.entity.StatusEvent;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.tracking.dto.TimelineEventResponse;
import com.fooddelivery.tracking.entity.DriverLocationHistory;
import com.fooddelivery.tracking.repository.DriverLocationHistoryRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrackingServiceImpl implements TrackingService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DeliveryPartnerRepository partnerRepository;
    private final DriverLocationHistoryRepository historyRepository;

    public TrackingServiceImpl(OrderRepository orderRepository,
                               UserRepository userRepository,
                               DeliveryPartnerRepository partnerRepository,
                               DriverLocationHistoryRepository historyRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.partnerRepository = partnerRepository;
        this.historyRepository = historyRepository;
    }

    @Override
    public List<TimelineEventResponse> getVisibleTimeline(String orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Enforce subscription/access authorization
        boolean isOwner = order.getUserId() != null && order.getUserId().equals(user.getId());
        boolean isDriver = order.getDeliveryPartnerId() != null && order.getDeliveryPartnerId().equals(user.getId());
        boolean isAdmin = Role.ADMIN.equals(user.getRole());

        if (!isOwner && !isDriver && !isAdmin) {
            throw new SecurityException("Unauthorized access to order tracking");
        }

        List<TimelineEventResponse> timeline = new ArrayList<>();
        boolean isCustomer = Role.USER.equals(user.getRole());

        for (StatusEvent event : order.getStatusHistory()) {
            // Visibility Filter: Hide fraud check reviews (PENDING_REVIEW) from standard customer view
            if (isCustomer && event.getStatus() == OrderStatus.PENDING_REVIEW) {
                continue;
            }
            timeline.add(new TimelineEventResponse(
                    event.getStatus(),
                    event.getEventType(),
                    event.getTimestamp(),
                    event.getActorId(),
                    event.getActorType()
            ));
        }

        return timeline;
    }

    @Override
    @Transactional
    public void logLocationHistory(String driverId, String orderId, GeoJsonPoint location) {
        DriverLocationHistory history = new DriverLocationHistory(driverId, orderId, location);
        history.setTimestamp(LocalDateTime.now());
        historyRepository.save(history);
    }

    @Override
    public GeoJsonPoint getFilteredDriverLocation(String orderId, String email) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        // Enforce subscription/access authorization
        boolean isOwner = order.getUserId() != null && order.getUserId().equals(user.getId());
        boolean isDriver = order.getDeliveryPartnerId() != null && order.getDeliveryPartnerId().equals(user.getId());
        boolean isAdmin = Role.ADMIN.equals(user.getRole());

        if (!isOwner && !isDriver && !isAdmin) {
            throw new SecurityException("Unauthorized access to order tracking");
        }

        // LOCK driver visibility: GPS is only accessible to customers during active delivery transit (OUT_FOR_DELIVERY)
        if (order.getStatus() != OrderStatus.OUT_FOR_DELIVERY && order.getStatus() != OrderStatus.DELIVERED && !isAdmin) {
            return null;
        }

        if (order.getDeliveryPartnerId() == null) {
            return null;
        }

        Optional<DeliveryPartner> partnerOpt = partnerRepository.findById(order.getDeliveryPartnerId());
        return partnerOpt.map(DeliveryPartner::getCurrentLocation).orElse(null);
    }
}
