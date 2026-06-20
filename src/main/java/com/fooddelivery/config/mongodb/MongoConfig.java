package com.fooddelivery.config.mongodb;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB configuration.
 * Enables Mongo repositories across all feature modules and
 * activates auditing for @CreatedDate / @LastModifiedDate.
 */
@Configuration
@EnableMongoRepositories(basePackages = "com.fooddelivery")
@EnableMongoAuditing
public class MongoConfig {
}
