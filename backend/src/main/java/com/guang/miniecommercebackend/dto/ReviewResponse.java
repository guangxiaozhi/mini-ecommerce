package com.guang.miniecommercebackend.dto;

import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Integer rating;
    private String comment;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean edited;
    private boolean verifiedPurchase;

    public  ReviewResponse(){}
    public  ReviewResponse(Long id, Integer rating, String comment,String username,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           boolean edited, boolean verifiedPurchase){
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.username = username;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.edited = edited;
        this.verifiedPurchase = verifiedPurchase;
    }

    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    public Integer getRating(){return  rating;}
    public void setRating(Integer rating){this.rating = rating;}
    public String getComment(){return comment;}
    public void setComment(String comment){this.comment = comment;}
    public String getUsername(){return username;}
    public void setUsername(String username){this.username = username;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}
    public boolean isEdited(){return edited;}
    public void setEdited(boolean edited){this.edited = edited;}
    public boolean isVerifiedPurchase(){return verifiedPurchase;}
    public void setVerifiedPurchase(boolean verifiedPurchase){this.verifiedPurchase = verifiedPurchase;}
}
