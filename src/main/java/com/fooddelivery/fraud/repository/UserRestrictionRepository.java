package com.fooddelivery.fraud.repository;

import com.fooddelivery.fraud.entity.UserRestriction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRestrictionRepository extends MongoRepository<UserRestriction, String> {
    Optional<UserRestriction> findByUserId(String userId);
}
