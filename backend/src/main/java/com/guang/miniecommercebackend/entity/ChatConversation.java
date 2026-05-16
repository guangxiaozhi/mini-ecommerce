package com.guang.miniecommercebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "t_chat_conversation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_inquiry_user_product",
                columnNames = { "type", "product_id", "created_by_user_id" }
        )
)
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20 )
    private ChatConversationType type;

    @Column(name="product_id")
    private Long productId;

    @Column(name="order_id", unique = true)
    private Long orderId;

    @Column(name="created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId(){ return id; }
    public void setId(Long id) { this.id = id; }

    public ChatConversationType getType(){ return type; }
    public void setType(ChatConversationType type) { this.type = type; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

}