package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.entity.*;
import com.guang.miniecommercebackend.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock OrderRepository orderRepository;
    @Mock ReturnRequestRepository returnRequestRepository;
    @Mock ReturnItemRepository returnItemRepository;
    @Mock UserRepository userRepository;
    @Mock InventoryService inventoryService;

    @InjectMocks AdminOrderService adminOrderService;

    private Order buildOrder(Long id, OrderStatus status) {
        Order o = new Order();
        o.setId(id);
        o.setUserId(1L);
        o.setStatus(status);
        o.setTotalAmount(new BigDecimal("100.00"));
        return o;
    }

    private OrderItem buildItem(Long productId, int qty) {
        OrderItem i = new OrderItem();
        i.setProductId(productId);
        i.setQuantity(qty);
        i.setProductName("Product " + productId);
        i.setUnitPrice(BigDecimal.TEN);
        i.setLineTotal(BigDecimal.TEN);
        return i;
    }

    // ── updateStatus ──────────────────────────────────────────────────────────

    @Test
    void updateStatus_validTransition_changesStatusAndSaves() {
        Order order = buildOrder(1L, OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(orderRepository.save(any())).thenReturn(order);

        adminOrderService.updateStatus(1L, OrderStatus.PAID, 99L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        verify(orderRepository).save(order);
    }

    @Test
    void updateStatus_invalidTransition_throwsBadRequest() {
        Order order = buildOrder(1L, OrderStatus.PENDING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> adminOrderService.updateStatus(1L, OrderStatus.SHIPPED, 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid status transition");
    }

    @Test
    void updateStatus_toShipped_callsFulfillForEachItem() {
        Order order = buildOrder(1L, OrderStatus.PROCESSING);
        order.getItems().add(buildItem(10L, 2));
        order.getItems().add(buildItem(20L, 3));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(orderRepository.save(any())).thenReturn(order);

        adminOrderService.updateStatus(1L, OrderStatus.SHIPPED, 99L);

        verify(inventoryService).fulfill(10L, 2, 1L);
        verify(inventoryService).fulfill(20L, 3, 1L);
    }

    @Test
    void updateStatus_toCancelled_callsDeallocateForEachItem() {
        Order order = buildOrder(1L, OrderStatus.PENDING);
        order.getItems().add(buildItem(10L, 5));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(orderRepository.save(any())).thenReturn(order);

        adminOrderService.updateStatus(1L, OrderStatus.CANCELLED, 99L);

        verify(inventoryService).deallocate(10L, 5, 1L);
    }

    // ── approveReturn ─────────────────────────────────────────────────────────

    @Test
    void approveReturn_callsProcessReturnAndChangesStatus() {
        ReturnRequest rr = new ReturnRequest();
        rr.setId(1L);
        rr.setOrderId(10L);
        rr.setUserId(1L);
        rr.setStatus(ReturnStatus.REQUESTED);

        ReturnItem ri = new ReturnItem();
        ri.setProductId(100L);
        ri.setQuantity(2);
        ri.setReturnRequestId(1L);

        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(returnItemRepository.findByReturnRequestId(1L)).thenReturn(List.of(ri));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(returnRequestRepository.save(any())).thenReturn(rr);

        adminOrderService.approveReturn(1L, 99L);

        assertThat(rr.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        verify(inventoryService).processReturn(100L, 2, 1L);
    }

    // ── rejectReturn ──────────────────────────────────────────────────────────

    @Test
    void rejectReturn_changesStatusToRejected() {
        ReturnRequest rr = new ReturnRequest();
        rr.setId(1L);
        rr.setStatus(ReturnStatus.REQUESTED);
        rr.setUserId(1L);

        when(returnRequestRepository.findById(1L)).thenReturn(Optional.of(rr));
        when(userRepository.findById(1L)).thenReturn(Optional.of(new User()));
        when(returnItemRepository.findByReturnRequestId(1L)).thenReturn(List.of());
        when(returnRequestRepository.save(any())).thenReturn(rr);

        adminOrderService.rejectReturn(1L, "item not eligible", 99L);

        assertThat(rr.getStatus()).isEqualTo(ReturnStatus.REJECTED);
        verify(returnRequestRepository).save(rr);
    }
}
