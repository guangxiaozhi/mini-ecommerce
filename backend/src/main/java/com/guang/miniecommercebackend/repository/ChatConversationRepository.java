package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatConversation;
import com.guang.miniecommercebackend.entity.ChatConversationType;
import com.guang.miniecommercebackend.entity.ChatConversationStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long>{
    Optional<ChatConversation> findByOrderId(Long orderId);
    Optional<ChatConversation> findByTypeAndProductIdAndCreatedByUserId(
            ChatConversationType type,
            Long productId,
            Long createdByUserId);

//    查 某个状态 的所有会话
    Page<ChatConversation> findByStatusOrderByCreatedAtDesc(
            ChatConversationStatus status,
            Pageable pageable);

//    在「某状态」基础上再筛 会话类型
    Page<ChatConversation> findByStatusAndTypeOrderByCreatedAtDesc(
            ChatConversationStatus status,
            ChatConversationType type,
            Pageable pageable);
//    查看分配给我的会话
    Page<ChatConversation> findByAssignedAgentUserIdAndStatusOrderByCreatedAtDesc(
            Long assignedAgentUserId,
            ChatConversationStatus status,
            Pageable pageable);
//    和上面一样，但可以只查「我接的 ORDER 会话」或「我接的 INQUIRY 会话」。
    Page<ChatConversation> findByAssignedAgentUserIdAndStatusAndTypeOrderByCreatedAtDesc(
            Long assignedAgentUserId,
            ChatConversationStatus status,
            ChatConversationType type,
            Pageable pageable);
}
