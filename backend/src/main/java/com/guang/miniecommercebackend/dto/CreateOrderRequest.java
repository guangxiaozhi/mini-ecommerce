package com.guang.miniecommercebackend.dto;

/** 创建订单请求（按需加字段：地址、备注、发票等）。 */
public class CreateOrderRequest {
    private String shippingAddress;
    private String remark;

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}