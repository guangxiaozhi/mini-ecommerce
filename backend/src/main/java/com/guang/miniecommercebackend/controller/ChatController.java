package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.ChatConversationResponse;
import com.guang.miniecommercebackend.dto.ChatMessageResponse;
import com.guang.miniecommercebackend.dto.CreateChatConversationRequest;
import com.guang.miniecommercebackend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    // POST /api/chat/conversations        — 创建会话 + 参与者
    @PostMapping("/conversations")
//    @ResponseStatus(HttpStatus.CREATED) 不要写死 201
    public ResponseEntity<ChatConversationResponse> createConversation(
            Authentication auth,
            @Valid @RequestBody CreateChatConversationRequest body) {
        String username = (String) auth.getPrincipal();
        ChatService.CreateConversationOutcome outcome = chatService.createConversation(username, body);
        HttpStatus status = outcome.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(outcome.response());
    }
    // GET  /api/chat/conversations/{id}/messages?page=0&size=20 — 分页消息
    @GetMapping("/conversations/{conversationId}/messages")
    public Page<ChatMessageResponse> getMessages(
            Authentication auth,
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = (String) auth.getPrincipal();
        return chatService.getMessages(username, conversationId, page, size);
    }

}
