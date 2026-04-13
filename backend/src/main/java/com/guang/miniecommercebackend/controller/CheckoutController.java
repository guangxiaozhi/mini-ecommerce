package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.OrderDetailResponse;
import com.guang.miniecommercebackend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class CheckoutController {
    private final OrderService orderService;
    public CheckoutController(OrderService orderService){
        this.orderService = orderService;
    }

    //  下单 — POST /api/checkout
    @PostMapping("/checkout")
    //@ResponseStatus(HttpStatus.CREATED) 表示：这个方法正常返回时，HTTP 状态码用 201，而不是默认的 200。
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDetailResponse checkout(Authentication auth){
        String username = (String) auth.getPrincipal();
        return orderService.placeOrderFromCart(username);
    }
}
