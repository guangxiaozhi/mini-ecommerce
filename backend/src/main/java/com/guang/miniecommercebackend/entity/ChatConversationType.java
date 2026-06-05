package com.guang.miniecommercebackend.entity;

//定义“会话属于哪种业务场景”。Defines the business context of a conversation thread.
public enum ChatConversationType {
    ORDER, //Post-purchase conversation tied to a specific order
    INQUIRY //Pre-purchase inquiry conversation (product questions, stock, shipping, etc.)
}
