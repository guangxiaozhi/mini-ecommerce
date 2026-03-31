package com.guang.miniecommercebackend.dto;

import com.guang.miniecommercebackend.dto.CartItemResponse;
import java.math.BigDecimal;
import java.util.List;

//Response: full cart
public class CartResponse {
    private Long cartId;
    private List<CartItemResponse> items;
    private BigDecimal total; // sum of all subtotals
    private int itemCount;// total units (sum of quantities)

    public CartResponse(Long cartId, List<CartItemResponse> items,
                        BigDecimal total, int itemCount){
        this.cartId = cartId;
        this.items = items;
        this.total = total;
        this.itemCount = itemCount;
    }

    public Long getCartId() {return cartId;}
    public void setCartId(Long cartId) {this.cartId = cartId;}

    public List<CartItemResponse> getItems() {return items;}
    public void setItems(List<CartItemResponse> items) {this.items = items;}

    public BigDecimal getTotal() {return total;}
    public void setTotal(BigDecimal total) {this.total = total;}

    public int getItemCount() {return itemCount;}
    public void setItemCount(int itemCount) {this.itemCount = itemCount;}
}
