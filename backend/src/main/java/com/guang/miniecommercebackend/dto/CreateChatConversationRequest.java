//表示客户端要「创建会话」时发来的 JSON：会话类型、可选的订单号、商品号。只描述「输入」。
//客户端 POST /api/chat/conversations 时发来的 JSON，会反序列化成这个类。
package com.guang.miniecommercebackend.dto;

import com.guang.miniecommercebackend.entity.ChatConversationType;
import jakarta.validation.constraints.NotNull;

public class CreateChatConversationRequest {

    @NotNull
    private ChatConversationType type;

    private Long orderId;

    private Long productId;

    public ChatConversationType getType() {
        return type;
    }
    public void setType(ChatConversationType type) {
        this.type = type;
    }
    public Long getOrderId() {
        return orderId;
    }
    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }
    public Long getProductId() {
        return productId;
    }
    public void setProductId(Long productId) {
        this.productId = productId;
    }
}
