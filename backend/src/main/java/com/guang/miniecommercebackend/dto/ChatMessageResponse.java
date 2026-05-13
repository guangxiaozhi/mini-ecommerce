//分页查消息时，Page 里每一项用这个类型（和 Page<OrderSummaryResponse> 同理）。
package com.guang.miniecommercebackend.dto;

import java.time.LocalDateTime;

public class ChatMessageResponse {

    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String type;
    private String content;
    private LocalDateTime createdAt;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getConversationId() {
        return conversationId;
    }
    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }
    public Long getSenderUserId() {
        return senderUserId;
    }
    public void setSenderUserId(Long senderUserId) {
        this.senderUserId = senderUserId;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getContent() {
        return content;
    }
    public void setContent(String content) {
        this.content = content;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
