package com.guang.miniecommercebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SendChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
