package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrueOrderByIdAsc();

    Optional<Product> findByIdAndActiveTrue(Long id);
}
