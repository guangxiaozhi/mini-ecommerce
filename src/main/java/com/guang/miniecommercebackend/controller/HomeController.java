package com.guang.miniecommercebackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {
    @GetMapping("/products")
    public String home(){
        return "Products Coming soon!";
    @GetMapping("/")
    public String root() {
        return "mini-ecommerce-backend is running. GET /api/products for catalog.";
    }
}
