package com.fooddelivery.surge.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "daily_surge_summaries")
@CompoundIndexes({
        @CompoundIndex(name = "idx_h3_date", def = "{'h3Index': 1, 'date': -1}", unique = true)
})
public class DailySurgeSummary {

    @Id
    private String id;

    private String h3Index;
    private String date; // Format: "yyyy-MM-dd"

    private double avgMultiplier;
    private double maxMultiplier;
    private double surgeRevenue;
    private long surgeOrderCount;
    private LocalDateTime lastUpdatedAt;

    public DailySurgeSummary() {
        this.lastUpdatedAt = LocalDateTime.now();
        this.avgMultiplier = 1.0;
        this.maxMultiplier = 1.0;
        this.surgeRevenue = 0.0;
        this.surgeOrderCount = 0;
    }

    public DailySurgeSummary(String h3Index, String date) {
        this();
        this.h3Index = h3Index;
        this.date = date;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getH3Index() {
        return h3Index;
    }

    public void setH3Index(String h3Index) {
        this.h3Index = h3Index;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public double getAvgMultiplier() {
        return avgMultiplier;
    }

    public void setAvgMultiplier(double avgMultiplier) {
        this.avgMultiplier = avgMultiplier;
    }

    public double getMaxMultiplier() {
        return maxMultiplier;
    }

    public void setMaxMultiplier(double maxMultiplier) {
        this.maxMultiplier = maxMultiplier;
    }

    public double getSurgeRevenue() {
        return surgeRevenue;
    }

    public void setSurgeRevenue(double surgeRevenue) {
        this.surgeRevenue = surgeRevenue;
    }

    public long getSurgeOrderCount() {
        return surgeOrderCount;
    }

    public void setSurgeOrderCount(long surgeOrderCount) {
        this.surgeOrderCount = surgeOrderCount;
    }

    public LocalDateTime getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(LocalDateTime lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}
