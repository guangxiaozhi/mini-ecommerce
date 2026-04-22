package com.guang.miniecommercebackend.dto;

import com.guang.miniecommercebackend.dto.ReturnRequestResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 单个订单详情（含行明细）。 */
public class OrderDetailResponse {
    private Long id;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private List<ReturnRequestResponse> returnRequests = new ArrayList<>();

    public OrderDetailResponse() {}

    public OrderDetailResponse(Long id, BigDecimal totalAmount, String status,
                               LocalDateTime createdAt, List<OrderItemResponse> items) {
        this.id = id;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.items = items;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<OrderItemResponse> getItems() { return items; }
    public void setItems(List<OrderItemResponse> items) { this.items = items; }

    public List<ReturnRequestResponse> getReturnRequests(){return returnRequests;}
    public void setReturnRequests(List<ReturnRequestResponse> r){this.returnRequests = r;}
}