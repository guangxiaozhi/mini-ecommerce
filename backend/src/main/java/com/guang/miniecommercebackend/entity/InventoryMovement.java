package com.guang.miniecommercebackend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_inventory_movement")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 20)
    private MovementType movementType;

    // Positive for all types except ADJUST (which can be negative)
    @Column(name = "qty_change", nullable = false)
    private int qtyChange;

    // Only populated on RECEIVE movements, null otherwise
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    // Snapshot of all three buckets after this movement
    @Column(name = "on_hand_after", nullable = false)
    private int onHandAfter;

    @Column(name = "allocated_after", nullable = false)
    private int allocatedAfter;

    @Column(name = "available_after", nullable = false)
    private int availableAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", length = 20)
    private MovementReferenceType referenceType;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // null = system-triggered (checkout); non-null = admin user ID
    @Column(name = "created_by")
    private Long createdBy;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public MovementType getMovementType() { return movementType; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public int getQtyChange() { return qtyChange; }
    public void setQtyChange(int qtyChange) { this.qtyChange = qtyChange; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public int getOnHandAfter() { return onHandAfter; }
    public void setOnHandAfter(int onHandAfter) { this.onHandAfter = onHandAfter; }
    public int getAllocatedAfter() { return allocatedAfter; }
    public void setAllocatedAfter(int allocatedAfter) { this.allocatedAfter = allocatedAfter; }
    public int getAvailableAfter() { return availableAfter; }
    public void setAvailableAfter(int availableAfter) { this.availableAfter = availableAfter; }
    public MovementReferenceType getReferenceType() { return referenceType; }
    public void setReferenceType(MovementReferenceType r) { this.referenceType = r; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
