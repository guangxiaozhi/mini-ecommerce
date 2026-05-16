package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatParticipant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long>{
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    Page<ChatParticipant> findByUserIdOrderByJoinedAtDesc(Long userId, Pageable pageable);
}
