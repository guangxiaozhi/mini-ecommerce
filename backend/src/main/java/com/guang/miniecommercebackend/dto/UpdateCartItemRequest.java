package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// Request: update quantity
public class UpdateCartItemRequest {
    @NotNull
    @Min(1) @Max(99)
    private Integer quantity;

    public Integer getQuantity() {return quantity;}
    public void setQuantity(Integer quantity) {this.quantity = quantity;}
}