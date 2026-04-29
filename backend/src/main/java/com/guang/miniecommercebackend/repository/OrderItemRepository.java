package com.guang.miniecommercebackend.repository;
import  com.guang.miniecommercebackend.entity.OrderItem;
import  org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long>{
}
