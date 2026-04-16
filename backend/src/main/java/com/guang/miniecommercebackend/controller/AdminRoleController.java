package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.RoleResponse;
import com.guang.miniecommercebackend.service.AdminUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/roles")
public class AdminRoleController {

    private final AdminUserService adminUserService;

    public AdminRoleController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('USER_ROLES') or hasAuthority('USER_ADMIN_DETAIL') or hasAuthority('USER_REGULAR_DETAIL')))")
    @GetMapping
    public List<RoleResponse> listRoles() {
        return adminUserService.listRoles();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@RequestBody Map<String, Object> body) {
        String roleName = (String) body.get("roleName");
        String description = (String) body.get("description");
        boolean isAdminRole = Boolean.TRUE.equals(body.get("isAdminRole"));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminUserService.createRole(roleName, description, isAdminRole));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @PutMapping("/{id}")
    public RoleResponse updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String roleName = (String) body.get("roleName");
        String description = (String) body.get("description");
        boolean isAdminRole = Boolean.TRUE.equals(body.get("isAdminRole"));
        return adminUserService.updateRole(id, roleName, description, isAdminRole);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        adminUserService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @PostMapping("/{roleId}/users/{userId}")
    public RoleResponse addUserToRole(@PathVariable Long roleId, @PathVariable Long userId) {
        return adminUserService.addUserToRole(roleId, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @DeleteMapping("/{roleId}/users/{userId}")
    public RoleResponse removeUserFromRole(@PathVariable Long roleId, @PathVariable Long userId) {
        return adminUserService.removeUserFromRole(roleId, userId);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @PostMapping("/{roleId}/permissions/{permissionCode}")
    public RoleResponse addPermission(@PathVariable Long roleId, @PathVariable String permissionCode) {
        return adminUserService.addPermissionToRole(roleId, permissionCode);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('USER_ROLES'))")
    @DeleteMapping("/{roleId}/permissions/{permissionCode}")
    public RoleResponse removePermission(@PathVariable Long roleId, @PathVariable String permissionCode) {
        return adminUserService.removePermissionFromRole(roleId, permissionCode);
    }
}
