package com.guang.miniecommercebackend.dto;

import java.time.LocalDateTime;

public class ReviewEligibilityResponse {
    public enum Reason{NOT_OWNER, ORDER_NOT_DELIVERED, ORDER_ITEM_NOT_FOUND}
    public static class ExistingReview{
        private Long id;
        private Integer rating;
        private String comment;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
        private boolean deletedByAdmin;

        public  ExistingReview(){}
        public  ExistingReview(Long id, Integer rating, String comment,
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime deletedAt, boolean deletedByAdmin){
            this.id = id;
            this.rating = rating;
            this.comment = comment;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.deletedAt = deletedAt;
            this.deletedByAdmin = deletedByAdmin;
        }

        public Long getId(){return id;}
        public void setId(Long id){this.id = id;}
        public Integer getRating(){return rating;}
        public void setRating(Integer rating){this.rating = rating;}
        public String getComment(){return  comment;}
        public void setComment(String comment){this.comment = comment;}
        public LocalDateTime getCreatedAt(){return createdAt;}
        public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
        public LocalDateTime getUpdatedAt(){return  updatedAt;}
        public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}
        public LocalDateTime getDeletedAt(){return deletedAt;}
        public void setDeletedAt(LocalDateTime deletedAt){this.deletedAt = deletedAt;}
        public boolean isDeletedByAdmin(){return deletedByAdmin;}
        public void setDeletedByAdmin(boolean deletedByAdmin){this.deletedByAdmin = deletedByAdmin;}
    }

    private boolean eligible;
    private Reason reason; //null when dligible
    private ExistingReview existingReview; // null when none

    public ReviewEligibilityResponse(){}
    public ReviewEligibilityResponse(boolean eligible, Reason reason, ExistingReview existingReview){
        this.eligible = eligible;
        this.reason = reason;
        this.existingReview = existingReview;
    }

    public boolean isEligible(){return eligible;}
    public void setEligible(boolean eligible){this.eligible = eligible;}
    public Reason getReason(){return reason;}
    public void setReason(Reason reason){this.reason = reason;}
    public ExistingReview getExistingReview(){return existingReview;}
    public void setExistingReview(ExistingReview existingReview){this.existingReview = existingReview;}
}
