package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long>{
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);
}
