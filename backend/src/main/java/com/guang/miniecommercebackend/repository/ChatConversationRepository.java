package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long>{
    Optional<ChatConversation> findByOrderId(Long orderId);
}
