package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.ChatMessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatMessagePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatMessagePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishToConversation(Long conversationId, ChatMessageResponse message) {
        messagingTemplate.convertAndSend(
                "/topic/conversation." + conversationId,   // 频道名
                message                                    // JSON 消息体
        );
    }
}
