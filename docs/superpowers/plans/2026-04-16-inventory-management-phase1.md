# Inventory Management Phase 1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Inventory Management backend module — three-bucket model (on-hand / allocated / available) with per-batch cost tracking — and wire it into the existing checkout flow.

**Architecture:** One `Inventory` row per product tracks live bucket quantities using optimistic locking (`@Version`). Every mutation appends an immutable `InventoryMovement` audit row; RECEIVE movements additionally store `unitCost` for COGS/WAC calculations. `InventoryService` is the single write path; `OrderService` delegates stock changes to it. `Product.stock` is kept as a denormalized cache of `available_qty` so existing product-listing queries keep working without changes.

**Tech Stack:** Java 17, Spring Boot 4, Spring Data JPA (Hibernate), MySQL 8, JUnit 5 + Mockito (unit tests).

---

## File Map

### New files
| File | Purpose |
|---|---|
| `entity/MovementType.java` | Enum: RECEIVE, ALLOCATE, DEALLOCATE, FULFILL, RETURN, ADJUST |
| `entity/MovementReferenceType.java` | Enum: ORDER, RETURN, MANUAL |
| `entity/Inventory.java` | `t_inventory` — one row per product, three buckets + @Version |
| `entity/InventoryMovement.java` | `t_inventory_movement` — append-only audit log, unitCost on RECEIVE |
| `repository/InventoryRepository.java` | findByProductId, findByProductIdForUpdate (pessimistic) |
| `repository/InventoryMovementRepository.java` | paginated movements, WAC cost queries |
| `dto/InventoryResponse.java` | Snapshot DTO with costPrice + marginPct |
| `dto/MovementResponse.java` | Movement row DTO, unitCost visible on RECEIVE only |
| `dto/ReceiveStockRequest.java` | `{ qty, unitCost, note }` |
| `dto/AdjustStockRequest.java` | `{ delta, reason }` |
| `service/InventoryService.java` | All inventory mutations + queries |
| `controller/AdminInventoryController.java` | 5 admin endpoints under `/api/admin/inventory` |
| `service/InventoryServiceTest.java` | Unit tests (Mockito) for all service methods |

### Modified files
| File | Change |
|---|---|
| `entity/Product.java` | Add `costPrice DECIMAL(10,2)` nullable field |
| `config/ProductSampleDataLoader.java` | Seed `t_inventory` from `product.stock` after products exist |
| `service/OrderService.java` | Replace direct `product.setStock()` with `inventoryService.allocate()` |

All paths are relative to `backend/src/main/java/com/guang/miniecommercebackend/` (or `src/test/java/...` for tests).

---

## Task 1: Enums + Product.costPrice

**Files:**
- Create: `entity/MovementType.java`
- Create: `entity/MovementReferenceType.java`
- Modify: `entity/Product.java`

- [ ] **Step 1: Create MovementType enum**

```java
// backend/src/main/java/com/guang/miniecommercebackend/entity/MovementType.java
package com.guang.miniecommercebackend.entity;

public enum MovementType {
    RECEIVE,      // new stock arrives → on_hand+, available+
    ALLOCATE,     // order placed     → allocated+, available-
    DEALLOCATE,   // order cancelled  → allocated-, available+
    FULFILL,      // order shipped    → on_hand-, allocated-
    RETURN,       // return approved  → on_hand+, available+
    ADJUST        // manual correction → on_hand±, available±
}
```

- [ ] **Step 2: Create MovementReferenceType enum**

```java
// backend/src/main/java/com/guang/miniecommercebackend/entity/MovementReferenceType.java
package com.guang.miniecommercebackend.entity;

public enum MovementReferenceType {
    ORDER,
    RETURN,
    MANUAL
}
```

- [ ] **Step 3: Add costPrice to Product entity**

Open `entity/Product.java`. After the `stock` field, add:

```java
@Column(name = "cost_price", precision = 10, scale = 2)
private BigDecimal costPrice;
```

Add the getter and setter:

```java
public BigDecimal getCostPrice() { return costPrice; }
public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
```

Add `import java.math.BigDecimal;` if not already present.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/entity/MovementType.java \
        backend/src/main/java/com/guang/miniecommercebackend/entity/MovementReferenceType.java \
        backend/src/main/java/com/guang/miniecommercebackend/entity/Product.java
git commit -m "feat: add MovementType/MovementReferenceType enums and Product.costPrice"
```

---

## Task 2: Inventory Entity

**Files:**
- Create: `entity/Inventory.java`

- [ ] **Step 1: Create Inventory entity**

```java
// backend/src/main/java/com/guang/miniecommercebackend/entity/Inventory.java
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
    private Long version;

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
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/entity/Inventory.java
git commit -m "feat: add Inventory entity with three-bucket model and optimistic lock"
```

---

## Task 3: InventoryMovement Entity

**Files:**
- Create: `entity/InventoryMovement.java`

- [ ] **Step 1: Create InventoryMovement entity**

```java
// backend/src/main/java/com/guang/miniecommercebackend/entity/InventoryMovement.java
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

    // Magnitude of movement. For ADJUST this may be negative.
    // For all other types: positive integer representing quantity involved.
    @Column(name = "qty_change", nullable = false)
    private int qtyChange;

    // Populated on RECEIVE only. Null for all other movement types.
    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

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

    // null means system-triggered (e.g., from checkout)
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
    public void setReferenceType(MovementReferenceType referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
```

- [ ] **Step 2: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/entity/InventoryMovement.java
git commit -m "feat: add InventoryMovement audit entity with unitCost on RECEIVE"
```

---

## Task 4: Repositories

**Files:**
- Create: `repository/InventoryRepository.java`
- Create: `repository/InventoryMovementRepository.java`

- [ ] **Step 1: Create InventoryRepository**

```java
// backend/src/main/java/com/guang/miniecommercebackend/repository/InventoryRepository.java
package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productId = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
```

- [ ] **Step 2: Create InventoryMovementRepository**

```java
// backend/src/main/java/com/guang/miniecommercebackend/repository/InventoryMovementRepository.java
package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.InventoryMovement;
import com.guang.miniecommercebackend.entity.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Page<InventoryMovement> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    // Sum of (qtyChange × unitCost) across all RECEIVE movements — used for WAC numerator
    @Query("SELECT COALESCE(SUM(m.qtyChange * m.unitCost), 0) FROM InventoryMovement m " +
           "WHERE m.productId = :productId AND m.movementType = :type")
    BigDecimal sumWeightedCost(@Param("productId") Long productId,
                               @Param("type") MovementType type);

    // Total qty received — used for WAC denominator
    @Query("SELECT COALESCE(SUM(m.qtyChange), 0) FROM InventoryMovement m " +
           "WHERE m.productId = :productId AND m.movementType = :type")
    Integer sumQtyByType(@Param("productId") Long productId,
                         @Param("type") MovementType type);
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/repository/InventoryRepository.java \
        backend/src/main/java/com/guang/miniecommercebackend/repository/InventoryMovementRepository.java
git commit -m "feat: add InventoryRepository and InventoryMovementRepository with WAC queries"
```

---

## Task 5: DTOs

**Files:**
- Create: `dto/InventoryResponse.java`
- Create: `dto/MovementResponse.java`
- Create: `dto/ReceiveStockRequest.java`
- Create: `dto/AdjustStockRequest.java`

- [ ] **Step 1: Create InventoryResponse**

```java
// backend/src/main/java/com/guang/miniecommercebackend/dto/InventoryResponse.java
package com.guang.miniecommercebackend.dto;

import java.math.BigDecimal;

public class InventoryResponse {
    private Long productId;
    private String productName;
    private int onHandQty;
    private int allocatedQty;
    private int availableQty;
    private BigDecimal costPrice;    // from Product.costPrice (null until first RECEIVE)
    private BigDecimal sellingPrice; // from Product.price
    private BigDecimal marginPct;    // (sellingPrice - costPrice) / sellingPrice * 100, null if costPrice null

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public int getOnHandQty() { return onHandQty; }
    public void setOnHandQty(int onHandQty) { this.onHandQty = onHandQty; }
    public int getAllocatedQty() { return allocatedQty; }
    public void setAllocatedQty(int allocatedQty) { this.allocatedQty = allocatedQty; }
    public int getAvailableQty() { return availableQty; }
    public void setAvailableQty(int availableQty) { this.availableQty = availableQty; }
    public BigDecimal getCostPrice() { return costPrice; }
    public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
    public BigDecimal getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    public BigDecimal getMarginPct() { return marginPct; }
    public void setMarginPct(BigDecimal marginPct) { this.marginPct = marginPct; }
}
```

- [ ] **Step 2: Create MovementResponse**

```java
// backend/src/main/java/com/guang/miniecommercebackend/dto/MovementResponse.java
package com.guang.miniecommercebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MovementResponse {
    private Long id;
    private Long productId;
    private String movementType;
    private int qtyChange;
    private BigDecimal unitCost;   // non-null only when movementType == "RECEIVE"
    private int onHandAfter;
    private int allocatedAfter;
    private int availableAfter;
    private String referenceType;
    private Long referenceId;
    private String note;
    private LocalDateTime createdAt;
    private Long createdBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
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
    public String getReferenceType() { return referenceType; }
    public void setReferenceType(String referenceType) { this.referenceType = referenceType; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
}
```

- [ ] **Step 3: Create ReceiveStockRequest**

```java
// backend/src/main/java/com/guang/miniecommercebackend/dto/ReceiveStockRequest.java
package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ReceiveStockRequest {

    @NotNull
    @Min(1)
    private Integer qty;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal unitCost;

    private String note;

    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }
    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
```

- [ ] **Step 4: Create AdjustStockRequest**

```java
// backend/src/main/java/com/guang/miniecommercebackend/dto/AdjustStockRequest.java
package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AdjustStockRequest {

    @NotNull
    private Integer delta;   // signed: positive to add, negative to remove

    @NotBlank
    private String reason;

    public Integer getDelta() { return delta; }
    public void setDelta(Integer delta) { this.delta = delta; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
```

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/dto/InventoryResponse.java \
        backend/src/main/java/com/guang/miniecommercebackend/dto/MovementResponse.java \
        backend/src/main/java/com/guang/miniecommercebackend/dto/ReceiveStockRequest.java \
        backend/src/main/java/com/guang/miniecommercebackend/dto/AdjustStockRequest.java
git commit -m "feat: add inventory DTOs (InventoryResponse, MovementResponse, request bodies)"
```

---

## Task 6: InventoryService — Tests First

**Files:**
- Create: `backend/src/test/java/com/guang/miniecommercebackend/service/InventoryServiceTest.java`

- [ ] **Step 1: Write all unit tests**

```java
// backend/src/test/java/com/guang/miniecommercebackend/service/InventoryServiceTest.java
package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.entity.Inventory;
import com.guang.miniecommercebackend.entity.InventoryMovement;
import com.guang.miniecommercebackend.entity.MovementType;
import com.guang.miniecommercebackend.entity.Product;
import com.guang.miniecommercebackend.repository.InventoryMovementRepository;
import com.guang.miniecommercebackend.repository.InventoryRepository;
import com.guang.miniecommercebackend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock InventoryRepository inventoryRepository;
    @Mock InventoryMovementRepository movementRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks InventoryService inventoryService;

    private Inventory inv(int onHand, int allocated, int available) {
        Inventory i = new Inventory();
        i.setProductId(1L);
        i.setOnHandQty(onHand);
        i.setAllocatedQty(allocated);
        i.setAvailableQty(available);
        return i;
    }

    // ── allocate ──────────────────────────────────────────────────────────────

    @Test
    void allocate_increasesAllocatedAndDecreasesAvailable() {
        Inventory inventory = inv(100, 10, 90);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.allocate(1L, 5, 99L);

        assertThat(inventory.getAllocatedQty()).isEqualTo(15);
        assertThat(inventory.getAvailableQty()).isEqualTo(85);
        assertThat(inventory.getOnHandQty()).isEqualTo(100); // unchanged
    }

    @Test
    void allocate_appendsAllocateMovementWithNullUnitCost() {
        Inventory inventory = inv(100, 10, 90);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.allocate(1L, 5, 99L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        InventoryMovement m = captor.getValue();
        assertThat(m.getMovementType()).isEqualTo(MovementType.ALLOCATE);
        assertThat(m.getQtyChange()).isEqualTo(5);
        assertThat(m.getUnitCost()).isNull();
        assertThat(m.getReferenceId()).isEqualTo(99L);
    }

    @Test
    void allocate_throwsConflict_whenAvailableLessThanQty() {
        Inventory inventory = inv(10, 8, 2);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.allocate(1L, 5, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("insufficient");
    }

    // ── deallocate ────────────────────────────────────────────────────────────

    @Test
    void deallocate_decreasesAllocatedAndIncreasesAvailable() {
        Inventory inventory = inv(100, 20, 80);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.deallocate(1L, 10, 99L);

        assertThat(inventory.getAllocatedQty()).isEqualTo(10);
        assertThat(inventory.getAvailableQty()).isEqualTo(90);
        assertThat(inventory.getOnHandQty()).isEqualTo(100); // unchanged
    }

    @Test
    void deallocate_appendsDeallocateMovement() {
        Inventory inventory = inv(100, 20, 80);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.deallocate(1L, 10, 99L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.DEALLOCATE);
        assertThat(captor.getValue().getQtyChange()).isEqualTo(10);
    }

    // ── fulfill ───────────────────────────────────────────────────────────────

    @Test
    void fulfill_decreasesOnHandAndAllocated_availableUnchanged() {
        Inventory inventory = inv(100, 20, 80);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        inventoryService.fulfill(1L, 10, 99L);

        assertThat(inventory.getOnHandQty()).isEqualTo(90);
        assertThat(inventory.getAllocatedQty()).isEqualTo(10);
        assertThat(inventory.getAvailableQty()).isEqualTo(80); // unchanged
    }

    @Test
    void fulfill_appendsFulfillMovementWithNullUnitCost() {
        Inventory inventory = inv(100, 20, 80);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);

        inventoryService.fulfill(1L, 10, 99L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.FULFILL);
        assertThat(captor.getValue().getUnitCost()).isNull();
    }

    // ── processReturn ─────────────────────────────────────────────────────────

    @Test
    void processReturn_increasesOnHandAndAvailable() {
        Inventory inventory = inv(90, 10, 80);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.processReturn(1L, 5, 77L);

        assertThat(inventory.getOnHandQty()).isEqualTo(95);
        assertThat(inventory.getAvailableQty()).isEqualTo(85);
        assertThat(inventory.getAllocatedQty()).isEqualTo(10); // unchanged
    }

    // ── receiveStock ──────────────────────────────────────────────────────────

    @Test
    void receiveStock_increasesOnHandAndAvailable() {
        Inventory inventory = inv(50, 10, 40);
        Product product = new Product();
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        inventoryService.receiveStock(1L, 30, new BigDecimal("12.50"), "new shipment", 1L);

        assertThat(inventory.getOnHandQty()).isEqualTo(80);
        assertThat(inventory.getAvailableQty()).isEqualTo(70);
        assertThat(inventory.getAllocatedQty()).isEqualTo(10); // unchanged
    }

    @Test
    void receiveStock_updatesProductCostPrice() {
        Inventory inventory = inv(50, 10, 40);
        Product product = new Product();
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        inventoryService.receiveStock(1L, 30, new BigDecimal("12.50"), "new shipment", 1L);

        assertThat(product.getCostPrice()).isEqualByComparingTo("12.50");
        verify(productRepository, atLeastOnce()).save(product);
    }

    @Test
    void receiveStock_appendsReceiveMovementWithUnitCost() {
        Inventory inventory = inv(50, 10, 40);
        Product product = new Product();
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        inventoryService.receiveStock(1L, 30, new BigDecimal("12.50"), "new shipment", 1L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        InventoryMovement m = captor.getValue();
        assertThat(m.getMovementType()).isEqualTo(MovementType.RECEIVE);
        assertThat(m.getUnitCost()).isEqualByComparingTo("12.50");
        assertThat(m.getQtyChange()).isEqualTo(30);
        assertThat(m.getCreatedBy()).isEqualTo(1L);
    }

    // ── manualAdjust ──────────────────────────────────────────────────────────

    @Test
    void manualAdjust_positiveOrNegativeDeltaAdjustsBothOnHandAndAvailable() {
        Inventory inventory = inv(50, 10, 40);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.manualAdjust(1L, -5, "shrinkage", 1L);

        assertThat(inventory.getOnHandQty()).isEqualTo(45);
        assertThat(inventory.getAvailableQty()).isEqualTo(35);
    }

    @Test
    void manualAdjust_throwsBadRequest_whenResultWouldBeNegative() {
        Inventory inventory = inv(5, 0, 5);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.manualAdjust(1L, -10, "correction", 1L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("negative");
    }
}
```

- [ ] **Step 2: Run tests — expect compilation failure (InventoryService doesn't exist yet)**

```bash
cd /c/Users/niris/Documents/mini-ecommerce/backend
mvn test -Dtest=InventoryServiceTest -q 2>&1 | tail -20
```

Expected: compile error — `InventoryService cannot be resolved`. This confirms the tests are wired correctly.

---

## Task 7: InventoryService — Implementation

**Files:**
- Create: `service/InventoryService.java`

- [ ] **Step 1: Implement InventoryService**

```java
// backend/src/main/java/com/guang/miniecommercebackend/service/InventoryService.java
package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.InventoryResponse;
import com.guang.miniecommercebackend.dto.MovementResponse;
import com.guang.miniecommercebackend.entity.*;
import com.guang.miniecommercebackend.repository.InventoryMovementRepository;
import com.guang.miniecommercebackend.repository.InventoryRepository;
import com.guang.miniecommercebackend.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            InventoryMovementRepository movementRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
    }

    // ── Order Management hooks ────────────────────────────────────────────────

    @Transactional
    public void allocate(Long productId, int qty, Long orderId) {
        Inventory inv = getForUpdateOr404(productId);
        if (inv.getAvailableQty() < qty) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "insufficient available stock for product: " + productId);
        }
        inv.setAllocatedQty(inv.getAllocatedQty() + qty);
        inv.setAvailableQty(inv.getAvailableQty() - qty);
        inventoryRepository.save(inv);
        syncProductStock(productId, inv.getAvailableQty());
        appendMovement(productId, MovementType.ALLOCATE, qty, null,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.ORDER, orderId, null, null);
    }

    @Transactional
    public void deallocate(Long productId, int qty, Long orderId) {
        Inventory inv = getForUpdateOr404(productId);
        inv.setAllocatedQty(inv.getAllocatedQty() - qty);
        inv.setAvailableQty(inv.getAvailableQty() + qty);
        inventoryRepository.save(inv);
        syncProductStock(productId, inv.getAvailableQty());
        appendMovement(productId, MovementType.DEALLOCATE, qty, null,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.ORDER, orderId, null, null);
    }

    // available unchanged — stock was already removed from available when allocated
    @Transactional
    public void fulfill(Long productId, int qty, Long orderId) {
        Inventory inv = getForUpdateOr404(productId);
        inv.setOnHandQty(inv.getOnHandQty() - qty);
        inv.setAllocatedQty(inv.getAllocatedQty() - qty);
        inventoryRepository.save(inv);
        appendMovement(productId, MovementType.FULFILL, qty, null,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.ORDER, orderId, null, null);
    }

    @Transactional
    public void processReturn(Long productId, int qty, Long returnId) {
        Inventory inv = getForUpdateOr404(productId);
        inv.setOnHandQty(inv.getOnHandQty() + qty);
        inv.setAvailableQty(inv.getAvailableQty() + qty);
        inventoryRepository.save(inv);
        syncProductStock(productId, inv.getAvailableQty());
        appendMovement(productId, MovementType.RETURN, qty, null,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.RETURN, returnId, null, null);
    }

    // ── Admin operations ──────────────────────────────────────────────────────

    @Transactional
    public void receiveStock(Long productId, int qty, BigDecimal unitCost, String note, Long adminId) {
        Inventory inv = getForUpdateOr404(productId);
        inv.setOnHandQty(inv.getOnHandQty() + qty);
        inv.setAvailableQty(inv.getAvailableQty() + qty);
        inventoryRepository.save(inv);
        // update Product.costPrice to the latest received unit cost
        productRepository.findById(productId).ifPresent(p -> {
            p.setCostPrice(unitCost);
            productRepository.save(p);
        });
        syncProductStock(productId, inv.getAvailableQty());
        appendMovement(productId, MovementType.RECEIVE, qty, unitCost,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.MANUAL, null, note, adminId);
    }

    @Transactional
    public void manualAdjust(Long productId, int delta, String reason, Long adminId) {
        Inventory inv = getForUpdateOr404(productId);
        int newOnHand = inv.getOnHandQty() + delta;
        int newAvailable = inv.getAvailableQty() + delta;
        if (newOnHand < 0 || newAvailable < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "adjustment would result in negative stock");
        }
        inv.setOnHandQty(newOnHand);
        inv.setAvailableQty(newAvailable);
        inventoryRepository.save(inv);
        syncProductStock(productId, inv.getAvailableQty());
        appendMovement(productId, MovementType.ADJUST, delta, null,
                inv.getOnHandQty(), inv.getAllocatedQty(), inv.getAvailableQty(),
                MovementReferenceType.MANUAL, null, reason, adminId);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public InventoryResponse getInventory(Long productId) {
        Inventory inv = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "inventory not found for product: " + productId));
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "product not found: " + productId));
        return toResponse(inv, product);
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listAll(String keyword, Integer lowStockThreshold) {
        List<Inventory> inventories = inventoryRepository.findAll();
        Map<Long, Product> productMap = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        return inventories.stream()
                .filter(inv -> {
                    Product p = productMap.get(inv.getProductId());
                    if (p == null) return false;
                    if (keyword != null && !keyword.isBlank()
                            && !p.getName().toLowerCase().contains(keyword.toLowerCase())) return false;
                    if (lowStockThreshold != null && inv.getAvailableQty() > lowStockThreshold) return false;
                    return true;
                })
                .map(inv -> toResponse(inv, productMap.get(inv.getProductId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MovementResponse> getMovements(Long productId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository
                .findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toMovementResponse);
    }

    @Transactional(readOnly = true)
    public BigDecimal getWeightedAverageCost(Long productId) {
        BigDecimal totalCost = movementRepository.sumWeightedCost(productId, MovementType.RECEIVE);
        Integer totalQty = movementRepository.sumQtyByType(productId, MovementType.RECEIVE);
        if (totalQty == null || totalQty == 0) return BigDecimal.ZERO;
        return totalCost.divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalInventoryValue() {
        return inventoryRepository.findAll().stream()
                .map(inv -> productRepository.findById(inv.getProductId())
                        .filter(p -> p.getCostPrice() != null)
                        .map(p -> p.getCostPrice().multiply(BigDecimal.valueOf(inv.getOnHandQty())))
                        .orElse(BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Inventory getForUpdateOr404(Long productId) {
        return inventoryRepository.findByProductIdForUpdate(productId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "inventory not found for product: " + productId));
    }

    // Keeps Product.stock in sync with available_qty so existing product-listing queries stay correct
    private void syncProductStock(Long productId, int availableQty) {
        productRepository.findById(productId).ifPresent(p -> {
            p.setStock(availableQty);
            productRepository.save(p);
        });
    }

    private void appendMovement(Long productId, MovementType type, int qtyChange, BigDecimal unitCost,
                                int onHandAfter, int allocatedAfter, int availableAfter,
                                MovementReferenceType refType, Long refId, String note, Long createdBy) {
        InventoryMovement m = new InventoryMovement();
        m.setProductId(productId);
        m.setMovementType(type);
        m.setQtyChange(qtyChange);
        m.setUnitCost(unitCost);
        m.setOnHandAfter(onHandAfter);
        m.setAllocatedAfter(allocatedAfter);
        m.setAvailableAfter(availableAfter);
        m.setReferenceType(refType);
        m.setReferenceId(refId);
        m.setNote(note);
        m.setCreatedBy(createdBy);
        movementRepository.save(m);
    }

    private InventoryResponse toResponse(Inventory inv, Product product) {
        InventoryResponse r = new InventoryResponse();
        r.setProductId(inv.getProductId());
        r.setProductName(product.getName());
        r.setOnHandQty(inv.getOnHandQty());
        r.setAllocatedQty(inv.getAllocatedQty());
        r.setAvailableQty(inv.getAvailableQty());
        r.setCostPrice(product.getCostPrice());
        r.setSellingPrice(product.getPrice());
        if (product.getCostPrice() != null && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal margin = product.getPrice()
                    .subtract(product.getCostPrice())
                    .divide(product.getPrice(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
            r.setMarginPct(margin);
        }
        return r;
    }

    private MovementResponse toMovementResponse(InventoryMovement m) {
        MovementResponse r = new MovementResponse();
        r.setId(m.getId());
        r.setProductId(m.getProductId());
        r.setMovementType(m.getMovementType().name());
        r.setQtyChange(m.getQtyChange());
        r.setUnitCost(m.getUnitCost());
        r.setOnHandAfter(m.getOnHandAfter());
        r.setAllocatedAfter(m.getAllocatedAfter());
        r.setAvailableAfter(m.getAvailableAfter());
        r.setReferenceType(m.getReferenceType() != null ? m.getReferenceType().name() : null);
        r.setReferenceId(m.getReferenceId());
        r.setNote(m.getNote());
        r.setCreatedAt(m.getCreatedAt());
        r.setCreatedBy(m.getCreatedBy());
        return r;
    }
}
```

- [ ] **Step 2: Run tests — expect all to pass**

```bash
cd /c/Users/niris/Documents/mini-ecommerce/backend
mvn test -Dtest=InventoryServiceTest -q
```

Expected output: `BUILD SUCCESS` with 14 tests passing. If any fail, read the failure message and fix the implementation before continuing.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/service/InventoryService.java \
        backend/src/test/java/com/guang/miniecommercebackend/service/InventoryServiceTest.java
git commit -m "feat: implement InventoryService with three-bucket model and unitCost tracking (TDD)"
```

---

## Task 8: DataLoader Migration

**Files:**
- Modify: `config/ProductSampleDataLoader.java`

- [ ] **Step 1: Add InventoryRepository dependency and seedInventories method**

Add the import and constructor injection for `InventoryRepository`:

```java
import com.guang.miniecommercebackend.entity.Inventory;
import com.guang.miniecommercebackend.repository.InventoryRepository;
```

Add field:
```java
private final InventoryRepository inventoryRepository;
```

Update constructor to include `InventoryRepository inventoryRepository` as last parameter and assign it.

- [ ] **Step 2: Restructure run() to seed inventories after products**

Replace the current `run()` method body with:

```java
@Override
public void run(String... args) {
    seedRoleIfAbsent("ROLE_USER", "Standard customer account");
    seedRoleIfAbsent("ROLE_ADMIN", "Full admin access");

    if (productRepository.count() == 0) {
        // ── Product 1 ──
        Product p1 = new Product();
        p1.setName("示例笔记本");
        p1.setDescription("入门学习用笔记本，示例数据。");
        p1.setPrice(new BigDecimal("29.99"));
        p1.setStock(100);
        p1.setActive(true);
        productRepository.save(p1);
        // ... (keep all existing product seeding code, just wrap it in this if block)
        // ... p2, p3, images, bullets, shipping options — unchanged
    }

    // Always run — idempotent: only creates inventory rows that don't exist yet
    seedInventoriesFromProducts();
}

private void seedInventoriesFromProducts() {
    for (Product product : productRepository.findAll()) {
        if (inventoryRepository.findByProductId(product.getId()).isEmpty()) {
            Inventory inv = new Inventory();
            inv.setProductId(product.getId());
            inv.setOnHandQty(product.getStock());
            inv.setAllocatedQty(0);
            inv.setAvailableQty(product.getStock());
            inventoryRepository.save(inv);
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/config/ProductSampleDataLoader.java
git commit -m "feat: seed t_inventory from product.stock on startup (idempotent)"
```

---

## Task 9: Update OrderService to Use InventoryService

**Files:**
- Modify: `service/OrderService.java`

This task replaces the direct `product.setStock()` deduction with `inventoryService.allocate()`. The pessimistic product lock is kept for price snapshot consistency.

- [ ] **Step 1: Add InventoryService dependency to OrderService**

Add field and constructor injection:
```java
private final InventoryService inventoryService;
```

Update constructor to include `InventoryService inventoryService` and assign it.

- [ ] **Step 2: Update placeOrderFromCart — remove stock deduction, add allocate loop**

In `placeOrderFromCart()`, remove these lines entirely:
```java
// DELETE this entire loop:
for (CartItem cartItem : cart.getItems()){
    Long pid = cartItem.getProduct().getId();
    Product product = lockedById.get(pid);
    product.setStock(product.getStock() - cartItem.getQuantity());
    productRepository.save(product);
}
```

Also remove the `validateStock()` calls (they are now handled inside `inventoryService.allocate()`):
```java
// DELETE this loop:
for (CartItem item : cart.getItems()){
    Long pid = item.getProduct().getId();
    Product p = lockedById.get(pid);
    validateStock(p, item.getQuantity());
}
```

After `Order saved = orderRepository.save(order);`, add:

```java
// Allocate inventory in sorted product-ID order (prevents deadlocks)
for (Long productId : productIds) {
    int qty = cart.getItems().stream()
            .filter(ci -> ci.getProduct().getId().equals(productId))
            .mapToInt(CartItem::getQuantity)
            .sum();
    inventoryService.allocate(productId, qty, saved.getId());
}
```

- [ ] **Step 3: Remove the now-unused validateStock private method**

Delete the `validateStock` method from `OrderService`:
```java
// DELETE:
private void validateStock(Product product, int requestedQty) {
    if (product.getStock() < requestedQty) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "insufficient stock for product: " + product.getName());
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/service/OrderService.java
git commit -m "feat: wire checkout to use InventoryService.allocate() instead of direct stock deduction"
```

---

## Task 10: AdminInventoryController

**Files:**
- Create: `controller/AdminInventoryController.java`

- [ ] **Step 1: Create AdminInventoryController**

```java
// backend/src/main/java/com/guang/miniecommercebackend/controller/AdminInventoryController.java
package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.AdjustStockRequest;
import com.guang.miniecommercebackend.dto.InventoryResponse;
import com.guang.miniecommercebackend.dto.MovementResponse;
import com.guang.miniecommercebackend.dto.ReceiveStockRequest;
import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.repository.UserRepository;
import com.guang.miniecommercebackend.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {

    private final InventoryService inventoryService;
    private final UserRepository userRepository;

    public AdminInventoryController(InventoryService inventoryService, UserRepository userRepository) {
        this.inventoryService = inventoryService;
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('INVENTORY_VIEW'))")
    @GetMapping
    public List<InventoryResponse> listAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer lowStock) {
        return inventoryService.listAll(keyword, lowStock);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('INVENTORY_VIEW'))")
    @GetMapping("/{productId}")
    public InventoryResponse getInventory(@PathVariable Long productId) {
        return inventoryService.getInventory(productId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('INVENTORY_VIEW'))")
    @GetMapping("/{productId}/movements")
    public Page<MovementResponse> getMovements(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return inventoryService.getMovements(productId, page, size);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('INVENTORY_MANAGE'))")
    @PostMapping("/{productId}/receive")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResponse receiveStock(
            @PathVariable Long productId,
            @Valid @RequestBody ReceiveStockRequest req,
            Authentication auth) {
        Long adminId = resolveUserId(auth);
        inventoryService.receiveStock(productId, req.getQty(), req.getUnitCost(), req.getNote(), adminId);
        return inventoryService.getInventory(productId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('INVENTORY_MANAGE'))")
    @PostMapping("/{productId}/adjust")
    public InventoryResponse adjust(
            @PathVariable Long productId,
            @Valid @RequestBody AdjustStockRequest req,
            Authentication auth) {
        Long adminId = resolveUserId(auth);
        inventoryService.manualAdjust(productId, req.getDelta(), req.getReason(), adminId);
        return inventoryService.getInventory(productId);
    }

    private Long resolveUserId(Authentication auth) {
        String username = (String) auth.getPrincipal();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
```

- [ ] **Step 2: Verify the app compiles**

```bash
cd /c/Users/niris/Documents/mini-ecommerce/backend
mvn compile -q
```

Expected: `BUILD SUCCESS`. Fix any import or compilation errors before continuing.

- [ ] **Step 3: Run all tests**

```bash
mvn test -q
```

Expected: `BUILD SUCCESS`. All tests (including `InventoryServiceTest`) pass.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/guang/miniecommercebackend/controller/AdminInventoryController.java
git commit -m "feat: add AdminInventoryController with 5 endpoints under /api/admin/inventory"
```

---

## Self-Review

### Spec coverage check

| Spec requirement | Task that covers it |
|---|---|
| Three-bucket model: on-hand, allocated, available | Tasks 2, 7 |
| unitCost on RECEIVE movements | Tasks 3, 7 (`receiveStock`) |
| `Product.costPrice` updated on RECEIVE | Task 7 (`receiveStock` → `syncProductStock` + costPrice update) |
| WAC query for analytics | Task 4 (`InventoryMovementRepository`), Task 7 (`getWeightedAverageCost`) |
| Total inventory value | Task 7 (`getTotalInventoryValue`) |
| Admin list/detail/movements endpoints | Task 10 |
| Admin receive stock (with unitCost) | Task 10 |
| Admin manual adjust | Task 10 |
| DataLoader seeds t_inventory from product.stock | Task 8 |
| Checkout uses InventoryService.allocate() | Task 9 |
| Permission gating (INVENTORY_VIEW / INVENTORY_MANAGE) | Task 10 |
| Optimistic locking (@Version) | Task 2 |
| Pessimistic locking on mutations (findByProductIdForUpdate) | Tasks 4, 7 |

No gaps found.

### Type consistency check

- `InventoryService.allocate(Long, int, Long)` — matches usage in Task 9 OrderService update ✓
- `InventoryService.receiveStock(Long, int, BigDecimal, String, Long)` — matches Task 10 controller call ✓
- `InventoryService.manualAdjust(Long, int, String, Long)` — matches Task 10 controller call ✓
- `InventoryResponse` fields match `toResponse()` mapper in Task 7 ✓
- `MovementResponse` fields match `toMovementResponse()` mapper in Task 7 ✓
- `InventoryMovementRepository.sumWeightedCost(Long, MovementType)` — matches Task 7 `getWeightedAverageCost()` call ✓
