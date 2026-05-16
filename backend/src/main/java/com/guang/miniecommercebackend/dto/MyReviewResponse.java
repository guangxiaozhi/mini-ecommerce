package com.guang.miniecommercebackend.dto;

import java.time.LocalDateTime;

public class MyReviewResponse {
    private Long id;
    private Long productId;
    private String  productName;
    private Integer rating;
    private String  comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean edited;
    private boolean hiddenByAdmin;

    public MyReviewResponse(){}
    public MyReviewResponse(Long id, Long productId, String productName, Integer rating,
                            String comment, LocalDateTime createdAt, LocalDateTime updatedAt,
                            boolean edited, boolean hiddenByAdmin){
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.edited = edited;
        this.hiddenByAdmin = hiddenByAdmin;
    }

    public Long getId(){return id;}
    public void setId(Long id){this.id = id;}
    public Long getProductId(){return productId;}
    public void setProductId(Long productId){this.productId = productId;}
    public String getProductName(){return productName;}
    public void setProductName(String productName){this.productName = productName;}
    public Integer getRating(){return rating;}
    public void setRating(Integer rating){this.rating = rating;}
    public String getComment(){return comment;}
    public void setComment(String comment){this.comment = comment;}
    public LocalDateTime getCreatedAt(){return  createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}
    public boolean isEdited(){return edited;}
    public void setEdited(boolean edited){this.edited = edited;}
    public boolean isHiddenByAdmin(){return hiddenByAdmin;}
    public void setHiddenByAdmin(boolean hiddenByAdmin){this.hiddenByAdmin = hiddenByAdmin;}
}
