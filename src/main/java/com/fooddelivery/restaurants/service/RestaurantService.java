package com.fooddelivery.restaurants.service;

import com.fooddelivery.common.exception.ResourceNotFoundException;
import com.fooddelivery.common.response.PaginatedResponse;
import com.fooddelivery.restaurants.dto.*;
import com.fooddelivery.restaurants.entity.OperatingHour;
import com.fooddelivery.restaurants.entity.Restaurant;
import com.fooddelivery.restaurants.mapper.RestaurantMapper;
import com.fooddelivery.restaurants.repository.RestaurantRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GeoNearOperation;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.core.query.NearQuery;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RestaurantService {

    private final RestaurantRepository repository;
    private final RestaurantMapper mapper;
    private final MongoTemplate mongoTemplate;

    public RestaurantService(RestaurantRepository repository, RestaurantMapper mapper, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    // --- ADMIN OPERATIONS ---

    public RestaurantAdminResponse createRestaurant(CreateRestaurantRequest request) {
        Restaurant restaurant = mapper.toEntity(request);
        restaurant = repository.save(restaurant);
        return mapper.toAdminResponse(restaurant);
    }

    public RestaurantAdminResponse updateRestaurant(String id, UpdateRestaurantRequest request) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        if (restaurant.getIsDeleted()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }

        if (StringUtils.hasText(request.getName())) restaurant.setName(request.getName());
        if (request.getDescription() != null) restaurant.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getContactPersonName())) restaurant.setContactPersonName(request.getContactPersonName());
        if (StringUtils.hasText(request.getPhone())) restaurant.setPhone(request.getPhone());
        if (request.getEmail() != null) restaurant.setEmail(request.getEmail());
        if (StringUtils.hasText(request.getAddress())) restaurant.setAddress(request.getAddress());
        
        if (StringUtils.hasText(request.getCityCode())) restaurant.setCityCode(request.getCityCode().toUpperCase());
        if (StringUtils.hasText(request.getCityName())) restaurant.setCityName(request.getCityName());
        
        if (request.getCuisines() != null && !request.getCuisines().isEmpty()) {
            restaurant.setCuisines(request.getCuisines().stream().map(String::toLowerCase).collect(Collectors.toList()));
        }
        
        if (request.getIsVegetarian() != null) restaurant.setVegetarian(request.getIsVegetarian());
        if (request.getPriceRange() != null) restaurant.setPriceRange(request.getPriceRange());
        if (request.getAverageDeliveryTimeMinutes() != null) restaurant.setAverageDeliveryTimeMinutes(request.getAverageDeliveryTimeMinutes());
        if (request.getMinimumOrderAmount() != null) restaurant.setMinimumOrderAmount(request.getMinimumOrderAmount());
        
        if (request.getOperatingHours() != null && !request.getOperatingHours().isEmpty()) {
            restaurant.setOperatingHours(request.getOperatingHours().stream()
                    .map(h -> new OperatingHour(h.getDay().toUpperCase(), h.getOpenTime(), h.getCloseTime()))
                    .collect(Collectors.toList()));
        }
        
        if (request.getLongitude() != null && request.getLatitude() != null) {
            restaurant.setLocation(new GeoJsonPoint(request.getLongitude(), request.getLatitude()));
        }
        
        if (request.getImages() != null) restaurant.setImages(request.getImages());

        restaurant = repository.save(restaurant);
        return mapper.toAdminResponse(restaurant);
    }

    public void deactivateRestaurant(String id) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        if (restaurant.getIsDeleted()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        restaurant.setIsActive(false);
        repository.save(restaurant);
    }

    public void activateRestaurant(String id) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        if (restaurant.getIsDeleted()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        restaurant.setIsActive(true);
        repository.save(restaurant);
    }

    public void verifyRestaurant(String id) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        if (restaurant.getIsDeleted()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        restaurant.setIsVerified(true);
        repository.save(restaurant);
    }

    public void deleteRestaurant(String id) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        restaurant.setIsDeleted(true);
        repository.save(restaurant);
    }

    public RestaurantAdminResponse getRestaurantForAdmin(String id) {
        return mapper.toAdminResponse(findRestaurantOrThrow(id));
    }

    public PaginatedResponse<RestaurantAdminResponse> listRestaurantsForAdmin(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Restaurant> result = repository.findAll(pageable);
        List<RestaurantAdminResponse> content = result.getContent().stream()
                .map(mapper::toAdminResponse)
                .collect(Collectors.toList());
        return new PaginatedResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    // --- PUBLIC OPERATIONS ---

    private Criteria buildPublicGuard() {
        return Criteria.where("isActive").is(true)
                .and("isVerified").is(true)
                .and("isDeleted").is(false);
    }

    public RestaurantResponse getRestaurantPublic(String id) {
        Restaurant restaurant = findRestaurantOrThrow(id);
        if (!restaurant.getIsActive() || !restaurant.getIsVerified() || restaurant.getIsDeleted()) {
            throw new ResourceNotFoundException("Restaurant not found");
        }
        return mapper.toResponse(restaurant);
    }

    public PaginatedResponse<RestaurantResponse> searchRestaurants(RestaurantSearchRequest request) {
        Query query = new Query();
        Criteria criteria = buildPublicGuard();

        if (StringUtils.hasText(request.getCuisine())) {
            criteria.and("cuisines").in(request.getCuisine().toLowerCase());
        }
        if (StringUtils.hasText(request.getCityCode())) {
            criteria.and("cityCode").is(request.getCityCode().toUpperCase());
        }
        if (request.getMinRating() != null) {
            criteria.and("averageRating").gte(request.getMinRating());
        }
        if (request.getMaxDeliveryTime() != null) {
            criteria.and("averageDeliveryTimeMinutes").lte(request.getMaxDeliveryTime());
        }
        if (request.getPriceRange() != null) {
            criteria.and("priceRange").is(request.getPriceRange());
        }
        if (request.getVegetarianOnly() != null && request.getVegetarianOnly()) {
            criteria.and("isVegetarian").is(true);
        }

        query.addCriteria(criteria);

        if ("rating".equalsIgnoreCase(request.getSortBy())) {
            query.with(Sort.by(Sort.Direction.DESC, "averageRating"));
        } else if ("popularity".equalsIgnoreCase(request.getSortBy())) {
            query.with(Sort.by(Sort.Direction.DESC, "popularityScore"));
        } else if ("deliveryTime".equalsIgnoreCase(request.getSortBy())) {
            query.with(Sort.by(Sort.Direction.ASC, "averageDeliveryTimeMinutes"));
        }

        long total = mongoTemplate.count(query, Restaurant.class);
        
        query.with(PageRequest.of(request.getPage(), request.getSize()));
        List<RestaurantResponse> content = mongoTemplate.find(query, Restaurant.class)
                .stream().map(mapper::toResponse).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) total / request.getSize());
        return new PaginatedResponse<>(content, request.getPage(), request.getSize(), total, totalPages);
    }

    public PaginatedResponse<RestaurantResponse> searchByName(String name, int page, int size) {
        Query query = new Query();
        query.addCriteria(buildPublicGuard());
        
        if (StringUtils.hasText(name)) {
            query.addCriteria(TextCriteria.forDefaultLanguage().matchingAny(name));
        }

        long total = mongoTemplate.count(query, Restaurant.class);
        query.with(PageRequest.of(page, size));
        List<RestaurantResponse> content = mongoTemplate.find(query, Restaurant.class)
                .stream().map(mapper::toResponse).collect(Collectors.toList());
                
        int totalPages = (int) Math.ceil((double) total / size);
        return new PaginatedResponse<>(content, page, size, total, totalPages);
    }

    public PaginatedResponse<RestaurantResponse> searchNearby(NearbySearchRequest request) {
        Criteria criteria = buildPublicGuard();

        if (StringUtils.hasText(request.getCuisine())) {
            criteria.and("cuisines").in(request.getCuisine().toLowerCase());
        }
        if (request.getMinRating() != null) {
            criteria.and("averageRating").gte(request.getMinRating());
        }
        if (request.getPriceRange() != null) {
            criteria.and("priceRange").is(request.getPriceRange());
        }
        if (request.getVegetarianOnly() != null && request.getVegetarianOnly()) {
            criteria.and("isVegetarian").is(true);
        }
        if (request.getMaxDeliveryTime() != null) {
            criteria.and("averageDeliveryTimeMinutes").lte(request.getMaxDeliveryTime());
        }

        NearQuery nearQuery = NearQuery.near(new Point(request.getLongitude(), request.getLatitude()))
                .spherical(true)
                .maxDistance(request.getRadiusKm() * 1000.0)
                .query(Query.query(criteria));
        GeoNearOperation geoNear = Aggregation.geoNear(nearQuery, "distance");

        Aggregation agg = Aggregation.newAggregation(
                geoNear,
                Aggregation.skip((long) request.getPage() * request.getSize()),
                Aggregation.limit(request.getSize())
        );

        AggregationResults<Restaurant> results = mongoTemplate.aggregate(agg, "restaurants", Restaurant.class);
        List<RestaurantResponse> content = results.getMappedResults().stream()
                .map(mapper::toResponse).collect(Collectors.toList());

        // Note: MongoDB $geoNear in aggregation doesn't provide total elements easily without a facet.
        // Returning simple pagination fields.
        return new PaginatedResponse<>(content, request.getPage(), request.getSize(), content.size(), 1);
    }

    private Restaurant findRestaurantOrThrow(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id: " + id));
    }
}
