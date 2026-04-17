package com.guang.miniecommercebackend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "on_hand_qty", nullable = false)
    private int onHandQty;

    @Column(name = "allocated_qty", nullable = false)
    private int allocatedQty;

    @Column(name = "available_qty", nullable = false)
    private int availableQty;

    @Version
    private Long version; //optimistic locking

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getOnHandQty() { return onHandQty; }
    public void setOnHandQty(int onHandQty) { this.onHandQty = onHandQty; }
    public int getAllocatedQty() { return allocatedQty; }
    public void setAllocatedQty(int allocatedQty) { this.allocatedQty = allocatedQty; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public Long getVersion() { return version; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
