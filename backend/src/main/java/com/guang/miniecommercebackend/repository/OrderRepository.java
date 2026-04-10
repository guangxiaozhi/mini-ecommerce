package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 某用户的订单，最新的在前（用于「我的订单」列表）。
     * Spring Data 会根据方法名生成查询：WHERE user_id = ? ORDER BY created_at DESC
     */
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 按主键 + 用户查一条；没有则 empty。
     * 用于详情接口：必须同时匹配 id 和 userId，防止越权。
     */
    Optional<Order> findByIdAndUserId(Long id, Long userId);
}