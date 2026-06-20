package com.fooddelivery.tracking.repository;

import com.fooddelivery.tracking.entity.DriverLocationHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB repository for DriverLocationHistory documents.
 */
@Repository
public interface DriverLocationHistoryRepository extends MongoRepository<DriverLocationHistory, String> {

    List<DriverLocationHistory> findByDriverIdOrderByTimestampDesc(String driverId);

    List<DriverLocationHistory> findByOrderIdOrderByTimestampDesc(String orderId);
}
