package com.guang.miniecommercebackend.dto;
import  java.math.BigDecimal;

public class ReturnItemResponse {
    private Long id;
    private Long orderItemId;
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice(){return  unitPrice;}
    public void setUnitPrice(BigDecimal unitPrice){this.unitPrice = unitPrice;}
    public BigDecimal getLineTotal(){return  lineTotal;}
    public void  setLineTotal(BigDecimal lineTotal){this.lineTotal = lineTotal;}
}
