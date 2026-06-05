package com.guang.miniecommercebackend.entity;

public enum ChatConversationStatus {
    BOT,           // Bot is handling the chat (default)
    WAITING_HUMAN, // Customer asked for a human; waiting for an agent to pick up
    ASSIGNED,      // A human agent has taken the conversation
    CLOSED         // Conversation is closed (optional for later)
}
