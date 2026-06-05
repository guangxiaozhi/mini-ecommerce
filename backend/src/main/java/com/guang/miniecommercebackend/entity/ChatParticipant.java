package com.guang.miniecommercebackend.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="t_chat_participant", uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_participant_conversation_user",
        columnNames = { "conversation_id", "user_id" }
))
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="conversation_id", nullable = false)
    private Long conversationId;

    @Column(name="user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatParticipantRole role;

    @Column(name="joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    public void prePersist() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public ChatParticipantRole getRole() { return role; }
    public void setRole(ChatParticipantRole role) { this.role = role;}

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }

}
