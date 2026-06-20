package com.fooddelivery.recommendations.dto;

import jakarta.validation.constraints.NotNull;

public class RecommendationQueryRequest {

    @NotNull(message = "Delivery latitude is required")
    private Double deliveryLatitude;

    @NotNull(message = "Delivery longitude is required")
    private Double deliveryLongitude;

    private Integer page = 0;
    private Integer size = 20;

    private String modelVersion;
    private String rankingVersion;

    public RecommendationQueryRequest() {
    }

    public Double getDeliveryLatitude() { return deliveryLatitude; }
    public void setDeliveryLatitude(Double deliveryLatitude) { this.deliveryLatitude = deliveryLatitude; }

    public Double getDeliveryLongitude() { return deliveryLongitude; }
    public void setDeliveryLongitude(Double deliveryLongitude) { this.deliveryLongitude = deliveryLongitude; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public String getModelVersion() { return modelVersion; }
    public void setModelVersion(String modelVersion) { this.modelVersion = modelVersion; }

    public String getRankingVersion() { return rankingVersion; }
    public void setRankingVersion(String rankingVersion) { this.rankingVersion = rankingVersion; }
}
