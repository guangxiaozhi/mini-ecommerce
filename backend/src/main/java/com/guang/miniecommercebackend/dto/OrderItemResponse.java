package com.guang.miniecommercebackend.dto;

import java.math.BigDecimal;

/** 订单里的一行商品（返回用）。 */
public class OrderItemResponse {
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;   // 下单时的单价快照
    private Integer quantity;
    private BigDecimal subtotal;    // unitPrice × quantity

    public OrderItemResponse() {}

    public OrderItemResponse(Long productId, String productName, BigDecimal unitPrice,
                             Integer quantity, BigDecimal subtotal) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}