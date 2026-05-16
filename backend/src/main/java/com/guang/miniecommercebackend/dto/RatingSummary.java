package com.guang.miniecommercebackend.dto;

public class RatingSummary {
    private final Long productId;
    private final Double avgRating; // null/0 when count is 0; controlled by query
    private final Long reviewCount;

    public RatingSummary(Long productId, Double avgRating, Long reviewCount){
        this.productId = productId;
        this.avgRating = avgRating;
        this.reviewCount = reviewCount;
    }

    public Long getProductId(){return productId;}
    public Double getAvgRating(){return avgRating;}
    public Long getReviewCount(){return reviewCount;}
}
