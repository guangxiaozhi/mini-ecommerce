package com.guang.miniecommercebackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

//  /api/health 可用于 Render 健康检查
//  / 不再被这个控制器占用，后面就可以给 React index.html
    @GetMapping("/api/health")
    public String health() {
        return "OK";
    }

//    @GetMapping("/products")
//    public String home() {
//        return "Products Coming soon!";
//    }
//
//    @GetMapping("/")
//    public String root() {
//        return "mini-ecommerce-backend is running. GET /api/products for catalog.";
//    }
}
