package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.entity.*;
import com.guang.miniecommercebackend.repository.*;
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
        assertThat(inventory.getOnHandQty()).isEqualTo(100);
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
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.ALLOCATE);
        assertThat(captor.getValue().getQtyChange()).isEqualTo(5);
        assertThat(captor.getValue().getUnitCost()).isNull();
        assertThat(captor.getValue().getReferenceId()).isEqualTo(99L);
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
        assertThat(inventory.getOnHandQty()).isEqualTo(100);
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
        assertThat(inventory.getAvailableQty()).isEqualTo(80);
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
        assertThat(inventory.getAllocatedQty()).isEqualTo(10);
    }

    // ── receiveStock ──────────────────────────────────────────────────────────

    @Test
    void receiveStock_increasesOnHandAndAvailable() {
        Inventory inventory = inv(50, 10, 40);
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.receiveStock(1L, 30, new BigDecimal("12.50"), "new shipment", 1L);

        assertThat(inventory.getOnHandQty()).isEqualTo(80);
        assertThat(inventory.getAvailableQty()).isEqualTo(70);
        assertThat(inventory.getAllocatedQty()).isEqualTo(10);
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
        when(inventoryRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(inventory));
        when(inventoryRepository.save(any())).thenReturn(inventory);
        when(productRepository.findById(1L)).thenReturn(Optional.of(new Product()));

        inventoryService.receiveStock(1L, 30, new BigDecimal("12.50"), "new shipment", 1L);

        ArgumentCaptor<InventoryMovement> captor = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(captor.capture());
        assertThat(captor.getValue().getMovementType()).isEqualTo(MovementType.RECEIVE);
        assertThat(captor.getValue().getUnitCost()).isEqualByComparingTo("12.50");
        assertThat(captor.getValue().getQtyChange()).isEqualTo(30);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
    }

    // ── manualAdjust ──────────────────────────────────────────────────────────

    @Test
    void manualAdjust_adjustsBothOnHandAndAvailableByDelta() {
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
