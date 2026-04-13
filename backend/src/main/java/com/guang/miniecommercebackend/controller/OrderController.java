package com.guang.miniecommercebackend.controller;

//import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.guang.miniecommercebackend.service.OrderService;
import com.guang.miniecommercebackend.dto.OrderSummaryResponse;
import com.guang.miniecommercebackend.dto.OrderDetailResponse;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService){
        this.orderService = orderService;
    }

    //  订单列表 - GET /api/orders
    @GetMapping
    public List<OrderSummaryResponse> getOrders(Authentication auth){
        String username = (String) auth.getPrincipal();
        return orderService.listMyOrders(username);
    }

    //  订单详情 — GET /api/orders/{orderId}
    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrderDetail(Authentication auth, @PathVariable("orderId") Long orderId){
        String username = (String) auth.getPrincipal();
        return orderService.getMyOrder(username, orderId);
    }



}
