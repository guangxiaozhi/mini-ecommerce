package com.guang.miniecommercebackend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 「我的订单」列表里的一行（轻量）。 */
public class OrderSummaryResponse {
    private Long id;
    private BigDecimal totalAmount;
    private String status;          // 若实体用枚举，可在这里转成 String
    private LocalDateTime createdAt;

    public OrderSummaryResponse() {}

    public OrderSummaryResponse(Long id, BigDecimal totalAmount, String status, LocalDateTime createdAt) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}