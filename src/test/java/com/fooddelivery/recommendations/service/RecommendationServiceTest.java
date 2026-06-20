package com.fooddelivery.recommendations.service;

import com.fooddelivery.common.enums.Role;
import com.fooddelivery.recommendations.dto.RecommendationQueryRequest;
import com.fooddelivery.recommendations.dto.RecommendationQueryResponse;
import com.fooddelivery.recommendations.entity.RecommendationEvent;
import com.fooddelivery.recommendations.entity.UserRecommendationProfile;
import com.fooddelivery.recommendations.entity.UserRecommendationProfile.CuisineAffinity;
import com.fooddelivery.recommendations.entity.UserRecommendationProfile.RestaurantAffinity;
import com.fooddelivery.recommendations.repository.RecommendationEventRepository;
import com.fooddelivery.recommendations.repository.UserRecommendationProfileRepository;
import com.fooddelivery.restaurants.entity.Restaurant;
import com.fooddelivery.restaurants.repository.RestaurantRepository;
import com.fooddelivery.users.entity.User;
import com.fooddelivery.users.repository.UserRepository;
import com.fooddelivery.surge.service.SurgePricingService;
import com.fooddelivery.surge.service.SupplyAnalysisEngine;
import com.uber.h3core.H3Core;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTest {

    @Mock
    private UserRecommendationProfileRepository userProfileRepository;

    @Mock
    private RecommendationEventRepository eventRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private SetOperations<String, Object> setOperations;

    @Mock
    private SurgePricingService surgePricingService;

    @Mock
    private SupplyAnalysisEngine supplyEngine;

    @Mock
    private MongoTemplate mongoTemplate;

    private H3Core h3;
    private RecommendationServiceImpl recommendationService;

    private User user;
    private UserRecommendationProfile profile;
    private Restaurant r1;
    private Restaurant r2;

    @BeforeEach
    void setUp() throws IOException {
        h3 = H3Core.newInstance();
        recommendationService = new RecommendationServiceImpl(
                userProfileRepository,
                eventRepository,
                restaurantRepository,
                userRepository,
                redisTemplate,
                h3,
                surgePricingService,
                supplyEngine,
                mongoTemplate
        );

        user = new User();
        user.setId("user_1");
        user.setEmail("user@test.com");
        user.setRole(Role.USER);

        profile = new UserRecommendationProfile("user_1");
        profile.getCuisineAffinity().add(new CuisineAffinity("Indian", 0.80, LocalDateTime.now().minusDays(5)));
        profile.getCuisineAffinity().add(new CuisineAffinity("Chinese", 0.20, LocalDateTime.now().minusDays(5)));

        r1 = new Restaurant();
        r1.setId("rest_1");
        r1.setName("Indian Spices");
        r1.setCuisines(List.of("Indian"));
        r1.setPriceRange(2);
        r1.setLocation(new GeoJsonPoint(77.3898, 28.6282)); // Noida
        r1.setIsActive(true);
        r1.setPopularityScore(8.0);

        r2 = new Restaurant();
        r2.setId("rest_2");
        r2.setName("Dragon Wok");
        r2.setCuisines(List.of("Chinese"));
        r2.setPriceRange(1);
        r2.setLocation(new GeoJsonPoint(77.4100, 28.6450)); // ~2.5km away, different H3 cell
        r2.setIsActive(true);
        r2.setPopularityScore(4.0);
    }

    @Test
    void testUpdateUserAffinities_DecayAndAccumulation() {
        when(userProfileRepository.findByUserId("user_1")).thenReturn(Optional.of(profile));
        when(userProfileRepository.save(any(UserRecommendationProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Update affinities with Indian order
        recommendationService.updateUserAffinities("user_1", "rest_1", "Indian", 250.0);

        verify(userProfileRepository).save(argThat(savedProfile -> {
            assertNotNull(savedProfile.getCuisineAffinity());
            // Double check that restaurant loyalty is recorded
            assertEquals(1, savedProfile.getRestaurantAffinities().size());
            assertEquals("rest_1", savedProfile.getRestaurantAffinities().get(0).getRestaurantId());
            assertEquals(1, savedProfile.getRestaurantAffinities().get(0).getOrderCount());
            // Verify normalization keeps sum around 1.0
            double sum = savedProfile.getCuisineAffinity().stream().mapToDouble(CuisineAffinity::getScore).sum();
            assertEquals(1.0, sum, 0.01);
            return true;
        }));
    }

    @Test
    void testCalculateRecommendations_SurgeDampeningAndExplanations() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId("user_1")).thenReturn(Optional.of(profile));
        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(Restaurant.class))).thenReturn(List.of(r1, r2));
        
        // Mock redis candidates (empty)
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members(anyString())).thenReturn(Collections.emptySet());

        // Noida center coordinates
        RecommendationQueryRequest request = new RecommendationQueryRequest();
        request.setDeliveryLatitude(28.6282);
        request.setDeliveryLongitude(77.3898);
        request.setPage(0);
        request.setSize(10);

        // Mock Surge and Delivery
        when(supplyEngine.getAvailableDriversCount(anyString())).thenReturn(5L); // couriers available
        when(surgePricingService.getActiveSurgeMultiplier(anyString())).thenReturn(1.0); // no surge

        RecommendationQueryResponse response = recommendationService.calculateRecommendations(request, "user@test.com");

        assertNotNull(response);
        assertEquals(2, response.getRecommendations().size());
        assertEquals("rest_1", response.getRecommendations().get(0).getRestaurantId()); // Indian (score 0.8) ranks higher than Chinese (score 0.2)
        assertTrue(response.getRecommendations().get(0).getExplanation().contains("Indian"));

        // Test with heavy surge on rest_1
        reset(surgePricingService);
        String rest1H3 = h3.latLngToCellAddress(r1.getLocation().getY(), r1.getLocation().getX(), 8);
        String rest2H3 = h3.latLngToCellAddress(r2.getLocation().getY(), r2.getLocation().getX(), 8);
        when(surgePricingService.getActiveSurgeMultiplier(rest1H3)).thenReturn(3.0); // heavy surge on rest_1
        when(surgePricingService.getActiveSurgeMultiplier(rest2H3)).thenReturn(1.0); // no surge on rest_2

        response = recommendationService.calculateRecommendations(request, "user@test.com");
        // Because of the heavy surge, rest_1's feasibility is dampened, ranking it below rest_2
        assertEquals("rest_2", response.getRecommendations().get(0).getRestaurantId());
    }

    @Test
    void testAttributionLogs() {
        String eventId = "recommendation-event-uuid";
        RecommendationEvent event = new RecommendationEvent(eventId, "user_1", "8861892513fffff", "m1", "r1");
        
        when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(RecommendationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        recommendationService.logClick(eventId, "rest_1");

        verify(eventRepository).save(argThat(savedEvent -> {
            assertEquals(1, savedEvent.getClicks().size());
            assertEquals("rest_1", savedEvent.getClicks().get(0).getRestaurantId());
            return true;
        }));

        recommendationService.attributeOrderConversion(eventId, "order_99", "rest_1", 300.0);

        verify(eventRepository, times(2)).save(argThat(savedEvent -> {
            assertEquals(1, savedEvent.getConversions().size());
            assertEquals("rest_1", savedEvent.getConversions().get(0).getRestaurantId());
            assertEquals("order_99", savedEvent.getConversions().get(0).getOrderId());
            assertEquals(300.0, savedEvent.getConversions().get(0).getRevenue());
            return true;
        }));
    }

    @Test
    void testTelemetryOwnershipValidation_MismatchedUser() {
        String eventId = "recommendation-event-uuid";
        RecommendationEvent event = new RecommendationEvent(eventId, "user_1", "8861892513fffff", "m1", "r1");
        
        when(eventRepository.findByEventId(eventId)).thenReturn(Optional.of(event));

        // Mock security context with a different user
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("other@test.com");
        
        org.springframework.security.core.context.SecurityContext securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(securityContext);

        // Mock different user
        User otherUser = new User();
        otherUser.setId("user_2");
        otherUser.setEmail("other@test.com");
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));

        try {
            assertThrows(SecurityException.class, () -> {
                recommendationService.logClick(eventId, "rest_1");
            });
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
