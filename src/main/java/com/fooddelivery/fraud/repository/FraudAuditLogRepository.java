package com.fooddelivery.fraud.repository;

import com.fooddelivery.fraud.entity.FraudAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAuditLogRepository extends MongoRepository<FraudAuditLog, String> {
    List<FraudAuditLog> findByUserId(String userId);
}
