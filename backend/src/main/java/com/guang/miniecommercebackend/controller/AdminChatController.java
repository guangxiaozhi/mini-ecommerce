package com.guang.miniecommercebackend.controller;

import com.guang.miniecommercebackend.dto.ChatMessageResponse;
import com.guang.miniecommercebackend.dto.SendChatMessageRequest;
import com.guang.miniecommercebackend.service.AdminChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.data.domain.Page;
import com.guang.miniecommercebackend.dto.ChatConversationResponse;
import com.guang.miniecommercebackend.entity.ChatConversationStatus;
import com.guang.miniecommercebackend.entity.ChatConversationType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/chat")
public class AdminChatController {
    private final AdminChatService adminChatService;

    public AdminChatController(AdminChatService adminChatService) {
        this.adminChatService = adminChatService;
    }

//    会话列表 GET /conversations
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('CHAT_ORDER_VIEW_ASSIGNED') or hasAuthority('CHAT_INQUIRY_VIEW_ASSIGNED')))")
    @GetMapping("/conversations")
    public Page<ChatConversationResponse> listConversations(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "false") boolean assignedToMe,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String username = (String) auth.getPrincipal();

        ChatConversationStatus convStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                convStatus = ChatConversationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status: " + status);
            }
        }

        ChatConversationType convType = null;
        if (type != null && !type.isBlank()) {
            try {
                convType = ChatConversationType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid type: " + type);
            }
        }

        return adminChatService.listAdminConversations(
                username, convStatus, convType, assignedToMe, page, size);
    }

//    接单 POST /conversations/{id}/assign
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('CHAT_ORDER_REPLY') or hasAuthority('CHAT_INQUIRY_REPLY')))")
    @PostMapping("/conversations/{conversationId}/assign")
    public ChatConversationResponse assignConversation(
            Authentication auth,
            @PathVariable("conversationId") Long conversationId) {
        String username = (String) auth.getPrincipal();
        return adminChatService.assignConversation(username, conversationId);
    }

//    看消息 GET /conversations/{id}/messages
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('CHAT_ORDER_VIEW_ASSIGNED') or hasAuthority('CHAT_INQUIRY_VIEW_ASSIGNED')))")
    @GetMapping("/conversations/{conversationId}/messages")
    public Page<ChatMessageResponse> getMessages(
            Authentication auth,
            @PathVariable("conversationId") Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        String username = (String) auth.getPrincipal();
        return adminChatService.getMessagesForAdmin(username, conversationId, page, size);
    }
//    客服发消息 POST /conversations/{id}/messages
    @PreAuthorize("hasRole('ADMIN') or (hasRole('ADMIN_PANEL') and (hasAuthority('CHAT_ORDER_REPLY') or hasAuthority('CHAT_INQUIRY_REPLY')))")
    @PostMapping("/conversations/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            Authentication auth,
            @PathVariable("conversationId") Long conversationId,
            @Valid @RequestBody SendChatMessageRequest body) {
        String username = (String) auth.getPrincipal();
        return adminChatService.sendMessageAsAgent(username, conversationId, body);
    }
}


