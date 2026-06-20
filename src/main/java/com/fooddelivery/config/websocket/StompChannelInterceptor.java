package com.fooddelivery.config.websocket;

import com.fooddelivery.common.enums.Role;
import com.fooddelivery.orders.entity.Order;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import com.fooddelivery.restaurants.entity.Restaurant;
import com.fooddelivery.restaurants.repository.RestaurantRepository;
import com.fooddelivery.auth.security.JwtUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Component
public class StompChannelInterceptor implements ChannelInterceptor {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final RestaurantRepository restaurantRepository;
    private final JwtUtil jwtUtil;

    public StompChannelInterceptor(OrderRepository orderRepository,
                                   UserRepository userRepository,
                                   RestaurantRepository restaurantRepository,
                                   JwtUtil jwtUtil) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.restaurantRepository = restaurantRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null) {
            StompCommand command = accessor.getCommand();

            // 1. Authenticate connection on CONNECT frame
            if (StompCommand.CONNECT.equals(command)) {
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtUtil.validateToken(token)) {
                        String email = jwtUtil.extractEmail(token);
                        String role = jwtUtil.extractRole(token);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                email,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        // Register authenticated principal
                        accessor.setUser(authentication);
                    } else {
                        throw new SecurityException("Connection rejected: Invalid JWT token");
                    }
                } else {
                    throw new SecurityException("Connection rejected: Missing or invalid Authorization header");
                }
            }
            // 2. Enforce subscription controls on SUBSCRIBE frame
            else if (StompCommand.SUBSCRIBE.equals(command)) {
                String destination = accessor.getDestination();
                if (destination != null) {
                    Principal principal = accessor.getUser();
                    if (principal == null || principal.getName() == null) {
                        throw new SecurityException("Subscription rejected: Unauthenticated user");
                    }

                    // Order topic authorization: Only Order Owner, Assigned Driver, and Admin
                    if (destination.startsWith("/topic/orders/")) {
                        String orderId = destination.substring("/topic/orders/".length());
                        Optional<Order> orderOpt = orderRepository.findById(orderId);
                        if (orderOpt.isEmpty()) {
                            throw new SecurityException("Subscription rejected: Order not found");
                        }

                        Order order = orderOpt.get();
                        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
                        if (userOpt.isEmpty()) {
                            throw new SecurityException("Subscription rejected: User profile not found");
                        }

                        User user = userOpt.get();
                        boolean isOwner = order.getUserId() != null && order.getUserId().equals(user.getId());
                        boolean isDriver = order.getDeliveryPartnerId() != null && order.getDeliveryPartnerId().equals(user.getId());
                        boolean isAdmin = Role.ADMIN.equals(user.getRole());

                        if (!isOwner && !isDriver && !isAdmin) {
                            throw new SecurityException("Subscription rejected: Unauthorized access to order tracking");
                        }
                    }
                    // Restaurant topic authorization: Only merchant matching restaurant email or Admin
                    else if (destination.startsWith("/topic/restaurants/")) {
                        String restaurantId = destination.substring("/topic/restaurants/".length());
                        Optional<Restaurant> restaurantOpt = restaurantRepository.findById(restaurantId);
                        if (restaurantOpt.isEmpty()) {
                            throw new SecurityException("Subscription rejected: Restaurant not found");
                        }

                        Restaurant restaurant = restaurantOpt.get();
                        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
                        if (userOpt.isEmpty()) {
                            throw new SecurityException("Subscription rejected: User profile not found");
                        }

                        User user = userOpt.get();
                        boolean isMerchant = restaurant.getEmail() != null && restaurant.getEmail().equalsIgnoreCase(user.getEmail());
                        boolean isAdmin = Role.ADMIN.equals(user.getRole());

                        if (!isMerchant && !isAdmin) {
                            throw new SecurityException("Subscription rejected: Unauthorized access to restaurant updates");
                        }
                    }
                }
            }
        }
        return message;
    }
}
