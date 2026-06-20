package com.fooddelivery.fraud.repository;

import com.fooddelivery.fraud.entity.DailyFraudMetrics;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DailyFraudMetricsRepository extends MongoRepository<DailyFraudMetrics, String> {
}
