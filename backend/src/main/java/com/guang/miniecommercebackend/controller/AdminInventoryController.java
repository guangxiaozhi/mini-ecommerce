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

    public AdminInventoryController(InventoryService inventoryService,
                                    UserRepository userRepository) {
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
        inventoryService.receiveStock(productId, req.getQty(), req.getUnitCost(),
                req.getNote(), adminId);
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
