package com.guang.miniecommercebackend.repository;

import com.guang.miniecommercebackend.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

//import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long>{
//    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
    // 换成可以分页显示
    Page<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
}
