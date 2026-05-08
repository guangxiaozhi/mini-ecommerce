package com.guang.miniecommercebackend.entity;

//定义“消息本身的类别” Defines the message category inside a conversation.
public enum ChatMessageType {
    TEXT, //Normal user/customer-service text message
    SYSTEM //Auto-generated system message (e.g., order status changed)
}
