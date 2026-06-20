package com.fooddelivery.recommendations.service;

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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.geo.Point;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final Logger LOGGER = Logger.getLogger(RecommendationServiceImpl.class.getName());

    private final UserRecommendationProfileRepository userProfileRepository;
    private final RecommendationEventRepository eventRepository;
    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final H3Core h3;
    private final SurgePricingService surgePricingService;
    private final SupplyAnalysisEngine supplyEngine;
    private final MongoTemplate mongoTemplate;

    public RecommendationServiceImpl(UserRecommendationProfileRepository userProfileRepository,
                                     RecommendationEventRepository eventRepository,
                                     RestaurantRepository restaurantRepository,
                                     UserRepository userRepository,
                                     RedisTemplate<String, Object> redisTemplate,
                                     H3Core h3,
                                     SurgePricingService surgePricingService,
                                     SupplyAnalysisEngine supplyEngine,
                                     MongoTemplate mongoTemplate) {
        this.userProfileRepository = userProfileRepository;
        this.eventRepository = eventRepository;
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
        this.h3 = h3;
        this.surgePricingService = surgePricingService;
        this.supplyEngine = supplyEngine;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    @Transactional
    public RecommendationQueryResponse calculateRecommendations(RecommendationQueryRequest request, String userEmail) {
        // Security check: verify event ownership / user isolation
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            if (!auth.getName().equalsIgnoreCase(userEmail)) {
                throw new SecurityException("Access denied: Authenticated user mismatch");
            }
        }

        // 1. Lookup User and H3 coordinates
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userEmail));
        
        String userH3;
        try {
            userH3 = h3.latLngToCellAddress(request.getDeliveryLatitude(), request.getDeliveryLongitude(), 8);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid user coordinates provided");
        }

        // 2. Fetch User Profile (or create default)
        UserRecommendationProfile profile = userProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    UserRecommendationProfile newProfile = new UserRecommendationProfile(user.getId());
                    return userProfileRepository.save(newProfile);
                });

        // 3. First Stage: Candidate Retrieval (filter by distance < 10km using geospatial index)
        Point userPoint = new Point(request.getDeliveryLongitude(), request.getDeliveryLatitude());
        Query geoQuery = new Query(
                Criteria.where("location").nearSphere(userPoint).maxDistance(10000.0) // 10km in meters
                        .and("isActive").is(true)
                        .and("isVerified").is(true)
                        .and("isDeleted").is(false)
        );
        List<Restaurant> nearbyRestaurants = mongoTemplate.find(geoQuery, Restaurant.class);
        List<RestaurantCandidate> candidates = new ArrayList<>();

        for (Restaurant restaurant : nearbyRestaurants) {
            if (restaurant.getLocation() == null) {
                continue;
            }

            double distance = calculateHaversineDistance(
                    request.getDeliveryLatitude(), request.getDeliveryLongitude(),
                    restaurant.getLocation().getY(), restaurant.getLocation().getX()
            );

            if (distance <= 10.0) { // Limit to 10km radius
                candidates.add(new RestaurantCandidate(restaurant, distance));
            }
        }

        // 4. Load Collaborative Candidates from Redis with resiliency fallback
        Set<String> cfSet = Collections.emptySet();
        try {
            Set<Object> collaborativeIds = redisTemplate.opsForSet().members("cf:user:" + user.getId() + ":candidates");
            if (collaborativeIds != null) {
                cfSet = collaborativeIds.stream().map(Object::toString).collect(Collectors.toSet());
            }
        } catch (Exception e) {
            LOGGER.severe("Redis failed to retrieve collaborative candidates: " + e.getMessage());
        }

        // 5. Score Candidates
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        for (RestaurantCandidate candidate : candidates) {
            Restaurant rest = candidate.restaurant;
            
            // Resolve restaurant H3 Resolution 8 index
            String restH3 = "";
            try {
                restH3 = h3.latLngToCellAddress(rest.getLocation().getY(), rest.getLocation().getX(), 8);
            } catch (Exception e) {
                // fallback
            }

            // A. User Affinity Score (Cuisine match, Price match, Loyalty)
            double maxCuisineScore = rest.getCuisines().stream()
                    .mapToDouble(c -> getCuisineAffinityScore(profile, c))
                    .max().orElse(0.0);

            double priceScore = getPricePreferenceScore(profile, rest.getPriceRange());
            double loyaltyScore = getLoyaltyScore(profile, rest.getId());
            double sAffinity = (0.50 * maxCuisineScore * 100.0) + (0.30 * priceScore * 100.0) + (0.20 * loyaltyScore * 100.0);

            // B. Popularity Score
            double sPopularity = Math.min(100.0, rest.getPopularityScore() * 10.0);

            // C. Collaborative Filtering
            double sCollaborative = cfSet.contains(rest.getId()) ? 80.0 : 0.0;

            // D. Content-Based Similarity
            double sContent = maxCuisineScore * 100.0;

            // Base Score
            double baseScore = (0.40 * sAffinity) + (0.20 * sPopularity) + (0.25 * sCollaborative) + (0.15 * sContent);

            // E. Feasibility Multipliers
            double fDistance = Math.exp(-0.15 * candidate.distance);
            
            long availableCouriers = supplyEngine.getAvailableDriversCount(restH3);
            double fDelivery = (availableCouriers >= 1) ? 1.0 : 0.2;

            double surgeMultiplier = surgePricingService.getActiveSurgeMultiplier(restH3);
            surgeMultiplier = Math.max(1.0, surgeMultiplier); // Defensive Surge Guard
            double fSurge = 1.0 / Math.pow(surgeMultiplier, 1.5);

            double fContext = 1.0;
            if (hour >= 6 && hour <= 11) { // Breakfast
                if (rest.getCuisines().stream().anyMatch(c -> c.equalsIgnoreCase("Breakfast") || c.equalsIgnoreCase("South Indian"))) {
                    fContext = 1.25;
                }
            } else if (hour >= 18 && hour <= 23) { // Dinner
                if (rest.getCuisines().stream().anyMatch(c -> c.equalsIgnoreCase("Indian") || c.equalsIgnoreCase("Biryani"))) {
                    fContext = 1.15;
                }
            }

            // Final score calculation
            double finalScore = baseScore * fDistance * fDelivery * fSurge * fContext;
            candidate.score = Math.round(finalScore * 100.0) / 100.0;

            // Generate Explanations
            candidate.explanation = generateExplanation(profile, rest, sAffinity, sCollaborative, maxCuisineScore);
        }

        // Sort by final score descending
        candidates.sort((c1, c2) -> Double.compare(c2.score, c1.score));

        // 6. Diversity Re-Ranking & 10% Exploration Bucket
        List<RestaurantCandidate> diverseList = applyDiversityAndExploration(candidates, profile);

        // 7. Pagination
        int totalElements = diverseList.size();
        int pageSize = request.getSize() != null ? request.getSize() : 20;
        int pageNum = request.getPage() != null ? request.getPage() : 0;
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        int start = Math.min(pageNum * pageSize, totalElements);
        int end = Math.min(start + pageSize, totalElements);
        List<RestaurantCandidate> pageCandidates = diverseList.subList(start, end);

        // 8. Create recommendation event for attribution tracking
        String eventId = UUID.randomUUID().toString();
        RecommendationEvent event = new RecommendationEvent(
                eventId, user.getId(), userH3,
                request.getModelVersion() != null ? request.getModelVersion() : "als_collaborative_v1",
                request.getRankingVersion() != null ? request.getRankingVersion() : "mmr_diversity_v1"
        );

        List<RecommendationEvent.RenderedRestaurant> renderedList = new ArrayList<>();
        for (int i = 0; i < pageCandidates.size(); i++) {
            RestaurantCandidate rc = pageCandidates.get(i);
            renderedList.add(new RecommendationEvent.RenderedRestaurant(rc.restaurant.getId(), start + i, rc.score));
        }
        event.setRenderedRestaurants(renderedList);
        eventRepository.save(event);

        // Map candidates to DTO items
        List<RecommendationQueryResponse.RecommendationItem> items = pageCandidates.stream()
                .map(rc -> new RecommendationQueryResponse.RecommendationItem(
                        rc.restaurant.getId(),
                        rc.restaurant.getName(),
                        rc.restaurant.getCuisines(),
                        rc.restaurant.getPriceRange(),
                        Math.round(rc.distance * 10.0) / 10.0,
                        rc.restaurant.getAverageDeliveryTimeMinutes(),
                        surgePricingService.getActiveSurgeMultiplier(rc.restaurant.getLocation() != null ? h3IndexOrEmpty(rc.restaurant.getLocation().getY(), rc.restaurant.getLocation().getX()) : ""),
                        rc.score,
                        rc.explanation
                )).collect(Collectors.toList());

        return new RecommendationQueryResponse(items, pageNum, pageSize, totalPages, eventId);
    }

    private String h3IndexOrEmpty(double lat, double lng) {
        try {
            return h3.latLngToCellAddress(lat, lng, 8);
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    @Transactional
    public void logClick(String eventId, String restaurantId) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                RecommendationEvent event = eventRepository.findByEventId(eventId)
                        .orElseThrow(() -> new IllegalArgumentException("Recommendation event not found: " + eventId));
                
                validateEventOwnership(event);

                boolean alreadyClicked = event.getClicks().stream().anyMatch(c -> c.getRestaurantId().equals(restaurantId));
                if (!alreadyClicked) {
                    event.getClicks().add(new RecommendationEvent.ClickEvent(restaurantId, LocalDateTime.now()));
                    eventRepository.save(event);
                }
                return;
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    throw e;
                }
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Override
    @Transactional
    public void attributeOrderConversion(String recommendationEventId, String orderId, String restaurantId, double revenue) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                RecommendationEvent event = eventRepository.findByEventId(recommendationEventId)
                        .orElseThrow(() -> new IllegalArgumentException("Recommendation event not found: " + recommendationEventId));

                validateEventOwnership(event);

                boolean alreadyConverted = event.getConversions().stream().anyMatch(c -> c.getRestaurantId().equals(restaurantId));
                if (!alreadyConverted) {
                    event.getConversions().add(new RecommendationEvent.ConversionEvent(restaurantId, orderId, LocalDateTime.now(), revenue));
                    eventRepository.save(event);
                }
                return;
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    throw e;
                }
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    @Override
    @Transactional
    public void updateUserAffinities(String userId, String restaurantId, String cuisine, double orderAmount) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                UserRecommendationProfile profile = userProfileRepository.findByUserId(userId)
                        .orElse(new UserRecommendationProfile(userId));

                LocalDateTime now = LocalDateTime.now();

                // 1. Update cuisine affinities with exponential decay (7 days half life)
                double lambda = Math.log(2.0) / 7.0; // decay constant per day
                LocalDateTime lastUpdated = profile.getUpdatedAt() != null ? profile.getUpdatedAt() : now.minusDays(7); // Null-safe fallback
                double daysElapsed = (double) Duration.between(lastUpdated, now).toSeconds() / (24.0 * 3600.0);

                for (CuisineAffinity ca : profile.getCuisineAffinity()) {
                    double decayedScore = ca.getScore() * Math.exp(-lambda * daysElapsed);
                    ca.setScore(Math.max(0.01, decayedScore));
                    ca.setLastUpdatedAt(now);
                }

                // Add or increment the current ordered cuisine
                if (cuisine != null && !cuisine.trim().isEmpty()) {
                    boolean found = false;
                    for (CuisineAffinity ca : profile.getCuisineAffinity()) {
                        if (ca.getCuisine().equalsIgnoreCase(cuisine)) {
                            ca.setScore(Math.min(1.0, ca.getScore() + 0.20));
                            ca.setLastUpdatedAt(now);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        profile.getCuisineAffinity().add(new CuisineAffinity(cuisine, 0.20, now));
                    }
                }

                // Normalize cuisine weights to prevent values exceeding bounds
                double totalScore = profile.getCuisineAffinity().stream().mapToDouble(CuisineAffinity::getScore).sum();
                if (totalScore > 0) {
                    for (CuisineAffinity ca : profile.getCuisineAffinity()) {
                        ca.setScore(ca.getScore() / totalScore);
                    }
                }

                // 2. Update persistent restaurant affinities
                boolean restFound = false;
                for (RestaurantAffinity ra : profile.getRestaurantAffinities()) {
                    if (ra.getRestaurantId().equals(restaurantId)) {
                        ra.setOrderCount(ra.getOrderCount() + 1);
                        ra.setAffinityScore(Math.min(1.0, ra.getAffinityScore() + 0.15));
                        ra.setLastOrderedAt(now);
                        restFound = true;
                        break;
                    }
                }
                if (!restFound) {
                    profile.getRestaurantAffinities().add(new RestaurantAffinity(restaurantId, 1L, 0.15, now));
                }

                profile.setUpdatedAt(now);
                userProfileRepository.save(profile);
                return;
            } catch (org.springframework.dao.OptimisticLockingFailureException e) {
                if (attempt == maxRetries) {
                    LOGGER.warning("Failed to update affinities due to concurrent lock failure after " + maxRetries + " attempts");
                    throw e;
                }
                try { Thread.sleep(50); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void validateEventOwnership(RecommendationEvent event) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String currentEmail = authentication.getName();
            User currentUser = userRepository.findByEmail(currentEmail)
                    .orElseThrow(() -> new SecurityException("Authenticated user not found: " + currentEmail));
            if (!currentUser.getId().equals(event.getUserId())) {
                throw new SecurityException("Access denied: You do not own this recommendation event");
            }
        }
    }

    // Helper Methods

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double getCuisineAffinityScore(UserRecommendationProfile profile, String cuisine) {
        return profile.getCuisineAffinity().stream()
                .filter(ca -> ca.getCuisine().equalsIgnoreCase(cuisine))
                .mapToDouble(CuisineAffinity::getScore)
                .findFirst()
                .orElse(0.05); // baseline fallback
    }

    private double getPricePreferenceScore(UserRecommendationProfile profile, int priceRange) {
        UserRecommendationProfile.PricePreference preference = profile.getPricePreference();
        if (priceRange == 1) return preference.getTier1Ratio();
        if (priceRange == 2) return preference.getTier2Ratio();
        if (priceRange == 3) return preference.getTier3Ratio();
        if (priceRange == 4) return preference.getTier4Ratio();
        return 0.25;
    }

    private double getLoyaltyScore(UserRecommendationProfile profile, String restaurantId) {
        return profile.getRestaurantAffinities().stream()
                .filter(ra -> ra.getRestaurantId().equals(restaurantId))
                .mapToDouble(RestaurantAffinity::getAffinityScore)
                .findFirst()
                .orElse(0.0);
    }

    private String generateExplanation(UserRecommendationProfile profile, Restaurant rest, double sAffinity, double sCollaborative, double maxCuisineScore) {
        // Loyalty Check
        double loyaltyVal = getLoyaltyScore(profile, rest.getId());
        if (loyaltyVal > 0.40) {
            return "Order again from your favorite";
        }
        // Collaborative Check
        if (sCollaborative > 50.0) {
            return "Similar users also ordered";
        }
        // Cuisine Check
        if (maxCuisineScore > 0.50) {
            // Find matched cuisine name
            String cuisineName = rest.getCuisines().stream()
                    .filter(c -> getCuisineAffinityScore(profile, c) > 0.50)
                    .findFirst().orElse(rest.getCuisines().isEmpty() ? "cuisine" : rest.getCuisines().get(0));
            return "Matches your love for " + cuisineName;
        }
        // Fallback
        return "Trending in your area";
    }

    private List<RestaurantCandidate> applyDiversityAndExploration(List<RestaurantCandidate> sorted, UserRecommendationProfile profile) {
        if (sorted.isEmpty()) {
            return sorted;
        }

        List<RestaurantCandidate> diverseList = new ArrayList<>();
        List<RestaurantCandidate> pool = new ArrayList<>(sorted);

        // 1. Dynamic Cuisine Throttling
        // Max 2 consecutive same cuisines, Max 4 per 10 items
        Map<String, Integer> cuisineCounts = new HashMap<>();

        while (!pool.isEmpty()) {
            RestaurantCandidate selected = null;
            
            // Check consecutive and block constraints
            for (RestaurantCandidate candidate : pool) {
                String primaryCuisine = candidate.restaurant.getCuisines().isEmpty() ? "Unknown" : candidate.restaurant.getCuisines().get(0);
                
                boolean okConsecutive = true;
                int size = diverseList.size();
                if (size >= 2) {
                    String last1 = getPrimaryCuisine(diverseList.get(size - 1));
                    String last2 = getPrimaryCuisine(diverseList.get(size - 2));
                    if (last1.equalsIgnoreCase(primaryCuisine) && last2.equalsIgnoreCase(primaryCuisine)) {
                        okConsecutive = false;
                    }
                }

                int sizeBucket = diverseList.size();
                int currentBucketStart = (sizeBucket / 10) * 10;
                int currentCuisineCountInBucket = 0;
                for (int i = currentBucketStart; i < sizeBucket; i++) {
                    if (getPrimaryCuisine(diverseList.get(i)).equalsIgnoreCase(primaryCuisine)) {
                        currentCuisineCountInBucket++;
                    }
                }

                boolean okCap = currentCuisineCountInBucket < 4;

                if (okConsecutive && okCap) {
                    selected = candidate;
                    break;
                }
            }

            // Fallback: If no candidate fits without penalty, pick the top one from the pool (we cannot block everything)
            if (selected == null) {
                selected = pool.get(0);
            }

            diverseList.add(selected);
            pool.remove(selected);
        }

        // 2. 10% Exploration Bucket Slotting (Replace index 4 with a random trending/new restaurant)
        if (diverseList.size() >= 5) {
            // Find an exploration candidate (not in user's top cuisines)
            Set<String> userTopCuisines = profile.getCuisineAffinity().stream()
                    .filter(ca -> ca.getScore() > 0.30)
                    .map(CuisineAffinity::getCuisine)
                    .collect(Collectors.toSet());

            RestaurantCandidate explorationItem = null;
            for (RestaurantCandidate rc : sorted) {
                String primary = getPrimaryCuisine(rc);
                if (!userTopCuisines.contains(primary)) {
                    explorationItem = rc;
                    break;
                }
            }

            if (explorationItem != null && diverseList.contains(explorationItem)) {
                diverseList.remove(explorationItem);
                explorationItem.explanation = "New discovery for you";
                diverseList.add(4, explorationItem); // Slot exploration item at index 4 (5th element)
            }
        }

        return diverseList;
    }

    private String getPrimaryCuisine(RestaurantCandidate rc) {
        return rc.restaurant.getCuisines().isEmpty() ? "Unknown" : rc.restaurant.getCuisines().get(0);
    }

    // Candidate Helper Wrapper
    private static class RestaurantCandidate {
        Restaurant restaurant;
        double distance;
        double score;
        String explanation;

        RestaurantCandidate(Restaurant restaurant, double distance) {
            this.restaurant = restaurant;
            this.distance = distance;
        }
    }
}
