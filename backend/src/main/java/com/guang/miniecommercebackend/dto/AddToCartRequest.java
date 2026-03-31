package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

//Request: add item
public class AddToCartRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1) @Max(99)
    private Integer quantity;

    public Long getProductId() {return productId; }
    public void setProductId(Long productId) {this.productId = productId;}

    public Integer getQuantity() {return quantity;}
    public void setQuantity(Integer quantity) {this.quantity = quantity;}
}
