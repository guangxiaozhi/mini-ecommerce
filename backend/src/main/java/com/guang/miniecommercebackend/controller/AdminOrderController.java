package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.*;
import com.guang.miniecommercebackend.entity.OrderStatus;
import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.repository.UserRepository;
import com.guang.miniecommercebackend.service.AdminOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final UserRepository userRepository;

    public AdminOrderController(AdminOrderService adminOrderService,
                                UserRepository userRepository) {
        this.adminOrderService = adminOrderService;
        this.userRepository = userRepository;
    }

    // ── Order list & detail ───────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_VIEW_ALL'))")
    @GetMapping
    public Page<AdminOrderResponse> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderService.listOrders(status, userId, page, size);
    }

    // NOTE: /analytics and /returns must be declared before /{orderId} so Spring
    // does not match them as path variables.

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_ANALYTICS'))")
    @GetMapping("/analytics")
    public OrderAnalyticsResponse getAnalytics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return adminOrderService.getAnalytics(from, to);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_VIEW_ALL'))")
    @GetMapping("/{orderId}")
    public AdminOrderResponse getOrder(@PathVariable Long orderId) {
        return adminOrderService.getOrder(orderId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_STATUS_UPDATE'))")
    @PutMapping("/{orderId}/status")
    public AdminOrderResponse updateStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateStatusRequest req,
            Authentication auth) {
        OrderStatus newStatus;
        try { newStatus = OrderStatus.valueOf(req.getStatus().toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status: " + req.getStatus());
        }
        return adminOrderService.updateStatus(orderId, newStatus, resolveUserId(auth));
    }

    // ── Return management ─────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_RETURNS'))")
    @GetMapping("/returns")
    public Page<ReturnRequestResponse> listReturns(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderService.listReturns(status, page, size);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_RETURNS'))")
    @GetMapping("/returns/{returnId}")
    public ReturnRequestResponse getReturn(@PathVariable Long returnId) {
        return adminOrderService.getReturn(returnId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_RETURNS'))")
    @PutMapping("/returns/{returnId}/approve")
    public ReturnRequestResponse approveReturn(@PathVariable Long returnId, Authentication auth) {
        return adminOrderService.approveReturn(returnId, resolveUserId(auth));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_RETURNS'))")
    @PutMapping("/returns/{returnId}/reject")
    public ReturnRequestResponse rejectReturn(
            @PathVariable Long returnId,
            @Valid @RequestBody RejectReturnRequest req,
            Authentication auth) {
        return adminOrderService.rejectReturn(returnId, req.getReason(), resolveUserId(auth));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('ORDER_RETURNS'))")
    @PutMapping("/returns/{returnId}/refund")
    public ReturnRequestResponse confirmRefund(
            @PathVariable Long returnId,
            @Valid @RequestBody RefundRequest req,
            Authentication auth) {
        return adminOrderService.confirmRefund(returnId, req.getRefundAmount(), resolveUserId(auth));
    }

    private Long resolveUserId(Authentication auth) {
        String username = (String) auth.getPrincipal();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }
}
