package com.fooddelivery.surge.repository;

import com.fooddelivery.surge.entity.DailySurgeSummary;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DailySurgeSummaryRepository extends MongoRepository<DailySurgeSummary, String> {
    Optional<DailySurgeSummary> findByH3IndexAndDate(String h3Index, String date);
}
