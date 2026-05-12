package com.guang.miniecommercebackend.dto;

import java.time.LocalDateTime;

public class AdminReviewResponse {
    public enum Status{ACTIVE, HIDDEN_BY_ADMIN, DELETED_BY_USER}
    private Long id;
    private Long productId;
    private String productName;
    private Long userId;
    private String username;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Status status;

    public AdminReviewResponse(){}
    public AdminReviewResponse(Long id, Long productId, String productName,
                               Long userId, String username, Integer rating,
                               String comment, LocalDateTime createdAt,
                               LocalDateTime updatedAt, Status status){
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.userId = userId;
        this.username = username;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = status;
    }

    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    public Long getProductId(){return  productId;}
    public void setProductId(Long productId){this.productId = productId;}
    public String getProductName(){return productName;}
    public void setProductName(String productName){this.productName = productName;}
    public Long getUserId(){return userId;}
    public void setUserId(Long userId){this.userId = userId;}
    public String getUsername(){return username;}
    public void setUsername(String username){this.username = username;}
    public Integer getRating(){return rating;}
    public void setRating(Integer rating){this.rating = rating;}
    public String getComment(){return comment;}
    public void setComment(String comment){this.comment = comment;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}
    public Status getStatus(){return status;}
    public void setStatus(Status status){this.status = status;}
}
