package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.*;
import com.guang.miniecommercebackend.entity.User;
import com.guang.miniecommercebackend.entity.UserBlacklist;
import com.guang.miniecommercebackend.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // ── User Detail tab (admin or regular) ───────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @GetMapping
    public List<UserResponse> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) User.UserStatus status) {

        if (keyword != null && !keyword.isBlank()) {
            return adminUserService.searchByUsername(keyword);
        }
        if (status != null) {
            return adminUserService.listByStatus(status);
        }
        return adminUserService.listAllUsers();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @GetMapping("/{id}")
    public UserResponse getUser(@PathVariable Long id) {
        return adminUserService.getUserById(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest req) {
        UserResponse created = adminUserService.createUser(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest req) {
        return adminUserService.updateUser(id, req);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @GetMapping("/{id}/addresses")
    public List<UserAddressResponse> getAddresses(@PathVariable Long id) {
        return adminUserService.getUserAddresses(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @GetMapping("/{id}/login-logs")
    public List<UserLoginLogResponse> getLoginLogs(@PathVariable Long id) {
        return adminUserService.getUserLoginLogs(id);
    }

    // ── Operation Log tab ─────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_OPERATION_LOG'))")
    @GetMapping("/{id}/operation-logs")
    public List<AdminOperationLogResponse> getOperationLogs(@PathVariable Long id) {
        return adminUserService.getOperationLogs(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_OPERATION_LOG'))")
    @GetMapping("/operation-logs")
    public List<AdminOperationLogResponse> getAllOperationLogs() {
        return adminUserService.getAllOperationLogs();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_OPERATION_LOG'))")
    @GetMapping("/login-logs")
    public List<UserLoginLogResponse> getAllLoginLogs() {
        return adminUserService.getAllLoginLogs();
    }

    // ── Blacklist tab ─────────────────────────────────────────────────────────

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_BLACKLIST'))")
    @PostMapping("/{id}/blacklist")
    public UserResponse blacklistUser(@PathVariable Long id, @RequestBody BlacklistRequest req) {
        return adminUserService.blacklistUser(id, req);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_BLACKLIST'))")
    @DeleteMapping("/{id}/blacklist")
    public UserResponse removeFromBlacklist(@PathVariable Long id) {
        return adminUserService.removeFromBlacklist(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_BLACKLIST'))")
    @GetMapping("/{id}/blacklist")
    public List<UserBlacklist> getBlacklistEntries(@PathVariable Long id) {
        return adminUserService.getUserBlacklistEntries(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_BLACKLIST'))")
    @GetMapping("/blacklist")
    public List<UserBlacklistResponse> getAllBlacklist() {
        return adminUserService.getAllBlacklistEntries();
    }
}
