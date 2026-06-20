package com.fooddelivery.fraud.service;

import com.fooddelivery.common.enums.Role;
import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import com.fooddelivery.fraud.entity.UserRestriction;
import com.fooddelivery.fraud.repository.DailyFraudMetricsRepository;
import com.fooddelivery.fraud.repository.UserRestrictionRepository;
import com.fooddelivery.fraud.repository.FraudAuditLogRepository;
import com.fooddelivery.orders.dto.OrderResponse;
import com.fooddelivery.orders.entity.*;
import com.fooddelivery.orders.mapper.OrderMapper;
import com.fooddelivery.orders.repository.OrderRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import com.fooddelivery.notifications.service.NotificationService;
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
public class FraudServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRestrictionRepository userRestrictionRepository;

    @Mock
    private DailyFraudMetricsRepository dailyMetricsRepository;

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Mock
    private FraudAuditLogRepository auditLogRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private FraudEvaluationServiceImpl fraudEvaluationService;

    @InjectMocks
    private FraudAdminServiceImpl fraudAdminService;

    private User customer;
    private User adminUser;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId("user_customer");
        customer.setEmail("customer@food.com");
        customer.setRole(Role.USER);
        customer.setCreatedAt(LocalDateTime.now().minusDays(10));

        adminUser = new User();
        adminUser.setId("user_admin");
        adminUser.setEmail("admin@food.com");
        adminUser.setRole(Role.ADMIN);

        order = new Order();
        order.setId("order_123");
        order.setUserId("user_customer");
        order.setStatus(OrderStatus.CREATED);
        order.setPricing(new OrderPricing(500.0, 0.0, 500.0, 25.0, 15.0, 30.0, 0.0, 570.0));
        order.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testEvaluateOrder_SafeScoreOutcome() {
        when(userRestrictionRepository.findByUserId("user_customer")).thenReturn(Optional.empty());
        when(orderRepository.findByUserIdAndCreatedAtAfter(eq("user_customer"), any())).thenReturn(new ArrayList<>());
        when(userRepository.findById("user_customer")).thenReturn(Optional.of(customer));

        fraudEvaluationService.evaluateOrder(order);

        assertEquals(0.0, order.getFraudDetails().getRiskScore());
        assertEquals(ReviewStatus.PASSED, order.getFraudDetails().getReviewStatus());
        assertEquals(OrderStatus.CREATED, order.getStatus());
    }

    @Test
    void testEvaluateOrder_BlockedIfUserHasOrderingRestriction() {
        UserRestriction restriction = new UserRestriction("user_customer", List.of("BLOCK_ORDERING"), "High risk behavior", LocalDateTime.now().plusDays(1), "SYSTEM");
        when(userRestrictionRepository.findByUserId("user_customer")).thenReturn(Optional.of(restriction));

        assertThrows(SecurityException.class, () -> {
            fraudEvaluationService.evaluateOrder(order);
        });
    }

    @Test
    void testEvaluateOrder_CriticalScoreOutcomeAndEscalationMatrix_1stOffense() {
        // Force critical score by triggering failed payment loop (50 pts) and velocity check (40 pts)
        List<Order> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Order failedOrder = new Order();
            failedOrder.setUserId("user_customer");
            failedOrder.setPaymentStatus(PaymentStatus.FAILED);
            failedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(2));
            history.add(failedOrder);
        }

        when(userRestrictionRepository.findByUserId("user_customer")).thenReturn(Optional.empty());
        when(orderRepository.findByUserIdAndCreatedAtAfter(eq("user_customer"), any())).thenReturn(history);
        when(userRepository.findById("user_customer")).thenReturn(Optional.of(customer));
        
        // Mock count of historical rejections as 0 (1st offense)
        when(orderRepository.countByUserIdAndFraudDetailsReviewStatus("user_customer", ReviewStatus.REJECTED)).thenReturn(0L);

        fraudEvaluationService.evaluateOrder(order);

        assertEquals(80.0, order.getFraudDetails().getRiskScore()); // 50 (failed payments) + 30 (velocity)
        assertEquals(ReviewStatus.REJECTED, order.getFraudDetails().getReviewStatus());
        assertEquals(OrderStatus.REJECTED, order.getStatus());

        // Verify Warning restriction is saved
        verify(userRestrictionRepository, times(1)).save(argThat(restriction -> 
            restriction.getRestrictionTypes().contains("WARNING") && restriction.getExpiresAt().isAfter(LocalDateTime.now())
        ));
    }

    @Test
    void testEvaluateOrder_CriticalScoreOutcomeAndEscalationMatrix_4thOffense() {
        List<Order> history = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Order failedOrder = new Order();
            failedOrder.setUserId("user_customer");
            failedOrder.setPaymentStatus(PaymentStatus.FAILED);
            failedOrder.setCreatedAt(LocalDateTime.now().minusMinutes(2));
            history.add(failedOrder);
        }

        when(userRestrictionRepository.findByUserId("user_customer")).thenReturn(Optional.empty());
        when(orderRepository.findByUserIdAndCreatedAtAfter(eq("user_customer"), any())).thenReturn(history);
        when(userRepository.findById("user_customer")).thenReturn(Optional.of(customer));
        
        // Mock count of historical rejections as 3 (4th offense)
        when(orderRepository.countByUserIdAndFraudDetailsReviewStatus("user_customer", ReviewStatus.REJECTED)).thenReturn(3L);

        fraudEvaluationService.evaluateOrder(order);

        // Verify BLOCK_ORDERING restriction is saved
        verify(userRestrictionRepository, times(1)).save(argThat(restriction -> 
            restriction.getRestrictionTypes().contains("BLOCK_ORDERING")
        ));
    }

    @Test
    void testApproveOrder_ReasonValidation() {
        // Approve must fail if reason is < 10 chars
        assertThrows(IllegalArgumentException.class, () -> {
            fraudAdminService.approveOrder("order_123", "admin@food.com", "short");
        });
    }

    @Test
    void testApproveOrder_Success() {
        order.setStatus(OrderStatus.PENDING_REVIEW);
        when(orderRepository.findById("order_123")).thenReturn(Optional.of(order));
        when(userRepository.findByEmail("admin@food.com")).thenReturn(Optional.of(adminUser));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(new OrderResponse());

        OrderResponse resp = fraudAdminService.approveOrder("order_123", "admin@food.com", "Override verified via phone call");

        assertNotNull(resp);
        assertEquals(OrderStatus.CREATED, order.getStatus());
        assertEquals(ReviewStatus.PASSED, order.getFraudDetails().getReviewStatus());
        assertEquals("Override verified via phone call", order.getFraudDetails().getAdminOverrideReason());
        verify(auditLogRepository, times(1)).save(any());
    }
}
