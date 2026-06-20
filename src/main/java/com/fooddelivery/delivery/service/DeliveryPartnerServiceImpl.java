package com.fooddelivery.delivery.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.delivery.dto.DeliveryPartnerResponse;
import com.fooddelivery.delivery.entity.DeliveryPartner;
import com.fooddelivery.delivery.entity.DeliveryPartnerStatus;
import com.fooddelivery.delivery.entity.VehicleType;
import com.fooddelivery.delivery.mapper.DeliveryPartnerMapper;
import com.fooddelivery.delivery.repository.DeliveryPartnerRepository;
import com.fooddelivery.orders.entity.EventType;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.entity.StatusEvent;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import com.fooddelivery.tracking.service.TrackingService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
public class DeliveryPartnerServiceImpl implements DeliveryPartnerService {

    private final DeliveryPartnerRepository repository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final DeliveryPartnerMapper mapper;
    private final MongoTemplate mongoTemplate;
    private final TrackingService trackingService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public DeliveryPartnerServiceImpl(DeliveryPartnerRepository repository,
                                      UserRepository userRepository,
                                      OrderRepository orderRepository,
                                      DeliveryPartnerMapper mapper,
                                      MongoTemplate mongoTemplate,
                                      TrackingService trackingService,
                                      SimpMessagingTemplate simpMessagingTemplate) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
        this.trackingService = trackingService;
        this.simpMessagingTemplate = simpMessagingTemplate;
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse updateAvailability(String email, DeliveryPartnerStatus status) {
        User user = findUserByEmailOrThrow(email);
        DeliveryPartner partner = getOrCreatePartner(user);

        // Guard: Cannot set status to OFFLINE if driver has active order
        if (status == DeliveryPartnerStatus.OFFLINE && partner.getCurrentOrderId() != null) {
            throw new IllegalStateException("Cannot go offline while on an active delivery assignment");
        }

        partner.setStatus(status);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse updateLocation(String email, Double latitude, Double longitude) {
        User user = findUserByEmailOrThrow(email);
        DeliveryPartner partner = getOrCreatePartner(user);

        GeoJsonPoint newLoc = new GeoJsonPoint(longitude, latitude);

        // GPS Spoofing validation: Speed check
        if (partner.getCurrentLocation() != null && partner.getLastLocationUpdateTime() != null) {
            double distMeters = calculateDistanceMeters(partner.getCurrentLocation(), newLoc);
            long seconds = ChronoUnit.SECONDS.between(partner.getLastLocationUpdateTime(), LocalDateTime.now());
            if (seconds > 0 && seconds < 300) { // check within 5 minutes
                double speedKmh = (distMeters / seconds) * 3.6;
                if (speedKmh > 120.0) {
                    throw new IllegalArgumentException("Spoofed GPS location update rejected. Speed exceeds physical limits (120 km/h)");
                }
            }
        }

        partner.setCurrentLocation(newLoc);
        partner.setLastLocationUpdateTime(LocalDateTime.now());
        partner = repository.save(partner);

        // Live GPS history dump and real-time STOMP topic streaming
        final String orderId = partner.getCurrentOrderId();
        if (orderId != null) {
            try {
                trackingService.logLocationHistory(partner.getId(), orderId, partner.getCurrentLocation());
                
                // Fetch the order to enforce driver visibility lock
                orderRepository.findById(orderId).ifPresent(order -> {
                    if (order.getStatus() == com.fooddelivery.orders.entity.OrderStatus.OUT_FOR_DELIVERY ||
                        order.getStatus() == com.fooddelivery.orders.entity.OrderStatus.DELIVERED) {
                        simpMessagingTemplate.convertAndSend(
                                "/topic/orders/" + orderId,
                                new com.fooddelivery.tracking.controller.TrackingController.DriverLocationResponse(latitude, longitude)
                        );
                    }
                });
            } catch (Exception e) {
                java.util.logging.Logger.getLogger(DeliveryPartnerServiceImpl.class.getName())
                        .warning("Logistics tracking updates failed: " + e.getMessage());
            }
        }

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse acceptAssignment(String email, String orderId) {
        User user = findUserByEmailOrThrow(email);
        DeliveryPartner partner = repository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (partner.getStatus() != DeliveryPartnerStatus.BUSY || !orderId.equals(partner.getCurrentOrderId())) {
            throw new IllegalStateException("Driver is not currently offered this order assignment");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Enforce Acceptance state transition
        partner.setStatus(DeliveryPartnerStatus.ON_DELIVERY);
        partner.setTotalAccepted(partner.getTotalAccepted() + 1);
        partner.setConsecutiveRejections(0);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        // Update Order log
        order.getStatusHistory().add(new StatusEvent(
                order.getStatus(),
                EventType.DELIVERY_PARTNER_ACTION,
                LocalDateTime.now(),
                partner.getId(),
                "DELIVERY_PARTNER"
        ));
        orderRepository.save(order);

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse rejectAssignment(String email, String orderId) {
        User user = findUserByEmailOrThrow(email);
        DeliveryPartner partner = repository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        if (partner.getStatus() != DeliveryPartnerStatus.BUSY || !orderId.equals(partner.getCurrentOrderId())) {
            throw new IllegalStateException("Driver is not currently offered this order assignment");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Rejection update
        partner.setTotalRejected(partner.getTotalRejected() + 1);
        partner.setConsecutiveRejections(partner.getConsecutiveRejections() + 1);
        
        if (partner.getConsecutiveRejections() >= 3) {
            partner.setStatus(DeliveryPartnerStatus.SUSPENDED);
        } else {
            partner.setStatus(DeliveryPartnerStatus.ONLINE);
        }

        partner.setCurrentOrderId(null);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        // Release Order
        order.setDeliveryPartnerId(null);
        order.getStatusHistory().add(new StatusEvent(
                order.getStatus(),
                EventType.DELIVERY_PARTNER_ACTION,
                LocalDateTime.now(),
                partner.getId(),
                "DELIVERY_PARTNER"
        ));
        orderRepository.save(order);

        return mapper.toResponse(partner);
    }

    @Override
    public DeliveryPartnerResponse getAssignedOrder(String email) {
        User user = findUserByEmailOrThrow(email);
        DeliveryPartner partner = repository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found"));

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse forceAssign(String driverId, String orderId) {
        DeliveryPartner partner = repository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Override assignment
        partner.setStatus(DeliveryPartnerStatus.ON_DELIVERY);
        partner.setCurrentOrderId(orderId);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        order.setDeliveryPartnerId(driverId);
        order.getStatusHistory().add(new StatusEvent(
                order.getStatus(),
                EventType.ADMIN_OVERRIDE,
                LocalDateTime.now(),
                "ADMIN",
                "ADMIN"
        ));
        orderRepository.save(order);

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse suspendDriver(String driverId) {
        DeliveryPartner partner = repository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        partner.setStatus(DeliveryPartnerStatus.SUSPENDED);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        return mapper.toResponse(partner);
    }

    @Override
    @Transactional
    public DeliveryPartnerResponse unsuspendDriver(String driverId) {
        DeliveryPartner partner = repository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found"));

        partner.setStatus(DeliveryPartnerStatus.OFFLINE);
        partner.setConsecutiveRejections(0);
        partner.setLastActiveTime(LocalDateTime.now());
        partner = repository.save(partner);

        return mapper.toResponse(partner);
    }

    private User findUserByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private DeliveryPartner getOrCreatePartner(User user) {
        return repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    DeliveryPartner partner = new DeliveryPartner();
                    partner.setUserId(user.getId());
                    partner.setStatus(DeliveryPartnerStatus.OFFLINE);
                    partner.setVehicleType(VehicleType.MOTORCYCLE);
                    return repository.save(partner);
                });
    }

    private double calculateDistanceMeters(GeoJsonPoint p1, GeoJsonPoint p2) {
        if (p1 == null || p2 == null) return 999999.0;
        double rx = p1.getX();
        double ry = p1.getY();
        double dx = p2.getX();
        double dy = p2.getY();
        return Math.sqrt(Math.pow(dx - rx, 2) + Math.pow(dy - ry, 2)) * 111000.0;
    }
}
