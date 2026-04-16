package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.ProductResponse;
import com.guang.miniecommercebackend.dto.ProductUpsertRequest;
import com.guang.miniecommercebackend.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('PRODUCT_BROWSE'))")
    @GetMapping
    public List<ProductResponse> listAll() {
        return productService.listAllProductsForAdmin();
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('PRODUCT_BROWSE'))")
    @GetMapping("/{id}")
    public ProductResponse getOne(@PathVariable Long id) {
        return productService.getProductForAdmin(id);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('PRODUCT_CREATE'))")
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductUpsertRequest req) {
        ProductResponse created = productService.createProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('PRODUCT_EDIT'))")
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpsertRequest req) {
        return productService.updateProduct(id, req);
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and hasAuthority('PRODUCT_DELETE'))")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
