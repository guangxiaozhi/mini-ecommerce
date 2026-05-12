package com.guang.miniecommercebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "t_review",
        uniqueConstraints = @UniqueConstraint(name = "uq_review_order_item", columnNames = "order_item_id"),
        indexes = {
                @Index(name = "ix_review_product_deleted", columnList = "product_id, deleted_at"),
                @Index(name = "ix_review_user_deleted", columnList = "user_id, deleted_at")
        }
)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false)
    private Long orderItemId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "product_id", nullable = false)
    private Long productID;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 1000)
    private String comment;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_admin", nullable = false)
    private boolean deletedByAdmin = false;

    @PrePersist
    public void prePersist(){
        LocalDateTime now = LocalDateTime.now();
        if(createdAt == null) createdAt = now;
        if(updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    public void preUpdate(){
        updatedAt = LocalDateTime.now();
    }

    public Long getId(){ return id;}
    public void setId(Long id){this.id = id;}
    public Long getOrderItemId(){return orderItemId;}
    public void setOrderItemId(Long orderItemId){this.orderItemId = orderItemId;}
    public Long getUserId(){return userId;}
    public void setUserId(Long userId){this.userId = userId;}
    public Long getProductID(){return productID;}
    public void setProductID(Long productID){this.productID = productID;}
    public Integer getRating(){return rating;}
    public void setRating(Integer rating){this.rating = rating;}
    public String getComment(){return comment;}
    public void setComment(String comment){this.comment = comment;}
    public LocalDateTime getCreatedAt(){return createdAt;}
    public void setCreatedAt(LocalDateTime createdAt){this.createdAt = createdAt;}
    public LocalDateTime getUpdatedAt(){return updatedAt;}
    public void setUpdatedAt(LocalDateTime updatedAt){this.updatedAt = updatedAt;}
    public LocalDateTime getDeletedAt(){return deletedAt;}
    public void setDeletedAt(LocalDateTime deletedAt){this.deletedAt = deletedAt;}
    public boolean isDeletedByAdmin(){return deletedByAdmin;}
    public void setDeletedByAdmin(boolean deletedByAdmin){this.deletedByAdmin = deletedByAdmin;}
}
