package com.fooddelivery.surge.repository;

import com.fooddelivery.surge.entity.SurgeOverride;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SurgeOverrideRepository extends MongoRepository<SurgeOverride, String> {
    List<SurgeOverride> findByH3IndexAndIsActiveTrueAndExpiresAtAfter(String h3Index, LocalDateTime now);
}
