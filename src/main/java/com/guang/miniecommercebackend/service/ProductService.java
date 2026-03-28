package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.ProductResponse;
import com.guang.miniecommercebackend.entity.Product;
import com.guang.miniecommercebackend.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> listActiveProducts() {
        return productRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getActiveProduct(Long id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found"));
        return toResponse(product);
    }

    private ProductResponse toResponse(Product p) {
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getPrice(),
                p.getStock(),
                p.getActive(),
                p.getCreatedAt()
        );
    }
}
