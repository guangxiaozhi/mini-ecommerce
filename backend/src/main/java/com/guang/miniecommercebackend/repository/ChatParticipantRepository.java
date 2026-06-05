package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatParticipant;
import com.guang.miniecommercebackend.entity.ChatParticipantRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long>{
    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    Page<ChatParticipant> findByUserIdOrderByJoinedAtDesc(Long userId, Pageable pageable);

    boolean existsByConversationIdAndRole(Long conversationId, ChatParticipantRole role);

//    该客服是否已接入此会话；发/看消息前的权限校验
    boolean existsByConversationIdAndUserIdAndRole(
            Long conversationId,
            Long userId,
            ChatParticipantRole role);
//    查「这个会话里，这个 user 有没有 participant 记录」
    Optional<ChatParticipant> findByConversationIdAndUserId(
            Long conversationId,
            Long userId);

    void deleteByConversationIdAndRole(Long conversationId, ChatParticipantRole role);

}
