package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.ProductResponse;
import com.guang.miniecommercebackend.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * GET /api/products — 只返回已上架（active=true）的商品。
     */
    @GetMapping
    public List<ProductResponse> list() {
        return productService.listActiveProducts();
    }

    /**
     * GET /api/products/{id} — 仅上架商品；不存在或已下架返回 404。
     */
    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getActiveProduct(id);
    }
}
