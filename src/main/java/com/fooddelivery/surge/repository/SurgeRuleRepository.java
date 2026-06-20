package com.fooddelivery.surge.repository;

import com.fooddelivery.surge.entity.SurgeRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SurgeRuleRepository extends MongoRepository<SurgeRule, String> {
    Optional<SurgeRule> findByZoneName(String zoneName);
}
