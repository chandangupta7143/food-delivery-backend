package com.fooddelivery.recommendations.repository;

import com.fooddelivery.recommendations.entity.UserRecommendationProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRecommendationProfileRepository extends MongoRepository<UserRecommendationProfile, String> {
    Optional<UserRecommendationProfile> findByUserId(String userId);
}
