package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.CartResponse;
import com.guang.miniecommercebackend.dto.UpdateCartItemRequest;
import com.guang.miniecommercebackend.dto.AddToCartRequest;
import com.guang.miniecommercebackend.service.CartService;

import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private  final CartService cartService;

    public CartController(CartService cartService){
        this.cartService = cartService;
    }

    /**
     * GET /api/cart
     * Returns the current user's cart (creates an empty one on first call).
     * 返回当前用户的购物车；首次调用自动创建空购物车。
     *
     * Authorization: Bearer <token>
     * Response 200: CartResponse { cartId, items[], total, itemCount }
     */
    @GetMapping
    public CartResponse getCart(Authentication auth) {
        String username = (String) auth.getPrincipal();
        return cartService.getCart(username);
    }

    /**
     * POST /api/cart/items
     * Add a product to the cart. Merges quantity if already present.
     * 将商品加入购物车；若已存在则合并数量，并校验库存。
     *
     * Authorization: Bearer <token>
     * Body: { "productId": 3, "quantity": 2 }
     * Response 201: updated CartResponse
     * Response 404: product not found or inactive
     * Response 409: insufficient stock
     */
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public CartResponse addItem(Authentication auth,
                                @Valid @RequestBody AddToCartRequest req) {
        String username = (String) auth.getPrincipal();
        return cartService.addItem(username, req);
    }

    /**
     * PUT /api/cart/items/{productId}
     * Overwrite the quantity of an existing cart item (does NOT merge).
     * 直接设置指定商品的数量（覆盖，不累加），校验库存。
     *
     * Authorization: Bearer <token>
     * Body: { "quantity": 5 }
     * Response 200: updated CartResponse
     * Response 404: product not in cart
     * Response 409: insufficient stock
     */
    @PutMapping("/items/{productId}")
    public CartResponse updateItem(Authentication auth,
                                   @PathVariable Long productId,
                                   @Valid @RequestBody
                                   UpdateCartItemRequest req) {
        String username = (String) auth.getPrincipal();
        return cartService.updateItem(username,productId,req);
    }

    /**
     * DELETE /api/cart/items/{productId}
     * Remove a single product line from the cart.
     * 从购物车中删除指定商品行。
     *
     * Authorization: Bearer <token>
     * Response 200: updated CartResponse (remaining items still visible)
     * Response 404: product not in cart
     */
    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(Authentication auth,
                                   @PathVariable Long productId) {
        String username = (String) auth.getPrincipal();
        return cartService.removeItem(username,productId);
    }

    /**
     * DELETE /api/cart
     * Wipe all items from the cart. The cart record itself is kept.
     * 清空购物车中所有商品行，购物车记录本身保留。
     *
     * Authorization: Bearer <token>
     * Response 204: No Content
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearCart(Authentication auth) {
        String username = (String) auth.getPrincipal();
        cartService.clearCart(username);
    }
}
