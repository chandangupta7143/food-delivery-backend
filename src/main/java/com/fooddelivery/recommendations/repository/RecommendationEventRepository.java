package com.fooddelivery.recommendations.repository;

import com.fooddelivery.recommendations.entity.RecommendationEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationEventRepository extends MongoRepository<RecommendationEvent, String> {
    Optional<RecommendationEvent> findByEventId(String eventId);
}
