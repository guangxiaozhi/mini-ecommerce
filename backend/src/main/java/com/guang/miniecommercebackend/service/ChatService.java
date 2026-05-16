package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.ChatConversationResponse;
import com.guang.miniecommercebackend.dto.ChatMessageResponse;
import com.guang.miniecommercebackend.dto.SendChatMessageRequest;
import com.guang.miniecommercebackend.entity.*;
import com.guang.miniecommercebackend.repository.ChatConversationRepository;
import com.guang.miniecommercebackend.repository.ChatMessageRepository;
import com.guang.miniecommercebackend.repository.ChatParticipantRepository;
import com.guang.miniecommercebackend.repository.UserRepository;
import com.guang.miniecommercebackend.repository.OrderRepository;
import com.guang.miniecommercebackend.repository.ProductRepository;
import com.guang.miniecommercebackend.dto.CreateChatConversationRequest;


import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Service
public class ChatService {
    private final ChatConversationRepository chatConversationRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public ChatService(ChatConversationRepository chatConversationRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository,
                       OrderRepository orderRepository,
                       ProductRepository productRepository
                       ){
        this.chatConversationRepository = chatConversationRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

//    按用户名取用户
    private User getUserByUsernameOr404(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found"));
    }

//    实体 → DTO 的两个私有映射方法
/* toConversationResponse(ChatConversation c) 做的是一件很具体的事：
    输入：已经保存在数据库里的 ChatConversation 实体 c（里面有 Long、enum、LocalDateTime 等）。
    输出：准备发给前端的 ChatConversationResponse DTO（JSON 里字段名、类型按 DTO 来，例如 type 用 字符串 "ORDER" / "INQUIRY"，而不是枚举对象）。
    为什么要多这一步？
    Controller / Service 对外返回 DTO，不直接返回实体，这样 API 长什么样由你控制，也和项目里 OrderSummaryResponse 那套一致。
    整个过程就是：新建一个空 DTO → 把实体里的值抄到 DTO 上 → 返回这个 DTO。
*/
    private ChatConversationResponse toConversationResponse(ChatConversation c){
        ChatConversationResponse r = new ChatConversationResponse();
        r.setId(c.getId());
        r.setType(c.getType().name());
        r.setOrderId(c.getOrderId());
        r.setProductId(c.getProductId());
        r.setCreatedByUserId(c.getCreatedByUserId());
        r.setCreatedAt(c.getCreatedAt());
        return r;
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m){
        ChatMessageResponse r = new ChatMessageResponse();
        r.setId(m.getId());
        r.setConversationId(m.getConversationId());
        r.setSenderUserId(m.getSenderUserId());
        r.setType(m.getType().name());
        r.setContent(m.getContent());
        r.setCreatedAt(m.getCreatedAt());
        return r;
    }

//    确保当前用户是参与者
//    干什么：同一会话里，当前登录用户若还没有参与者记录，就插一条 CUSTOMER。
//    用在哪里：新建会话后、以及「订单会话已存在（幂等）」时都会调用，避免只建了 t_chat_conversation 却没有 t_chat_participant。
    private void ensureCustomerParticipant(ChatConversation conv, User user) {
        if (!chatParticipantRepository.existsByConversationIdAndUserId(conv.getId(), user.getId())) {
            ChatParticipant p = new ChatParticipant();
            p.setConversationId(conv.getId());
            p.setUserId(user.getId());
            p.setRole(ChatParticipantRole.CUSTOMER);
            chatParticipantRepository.save(p);
        }
    }

    public record CreateConversationOutcome(boolean created, ChatConversationResponse response) {}

    @Transactional
    public CreateConversationOutcome createConversation(String username, CreateChatConversationRequest req) {
        User user = getUserByUsernameOr404(username);
        ChatConversation conv;
        if (req.getType() == ChatConversationType.ORDER) {
//            校验 orderId → 校验订单归属 → 有会话就复用并 created=false → 否则新建、created=true，也不再重复查库
            if (req.getOrderId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orderId is required for ORDER conversation");
            }
            Order order = orderRepository.findByIdAndUserId(req.getOrderId(), user.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found"));
            Optional<ChatConversation> existing = chatConversationRepository.findByOrderId(order.getId());
            if (existing.isPresent()) {
                conv = existing.get();
                ensureCustomerParticipant(conv, user);
                return new CreateConversationOutcome(false, toConversationResponse(conv));
            }

            ChatConversation c = new ChatConversation();
            c.setType(ChatConversationType.ORDER);
            c.setOrderId(order.getId());
            c.setCreatedByUserId(user.getId());
            conv = chatConversationRepository.save(c);

            ensureCustomerParticipant(conv, user);
            return new CreateConversationOutcome(true, toConversationResponse(conv));
        } else if (req.getType() == ChatConversationType.INQUIRY) {
            if (req.getProductId() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId is required for INQUIRY conversation");
            }
            if (!productRepository.existsById(req.getProductId())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "product not found");
            }

            Optional<ChatConversation> existingInquiry = chatConversationRepository
                    .findByTypeAndProductIdAndCreatedByUserId(
                            ChatConversationType.INQUIRY,
                            req.getProductId(),
                            user.getId());
            if (existingInquiry.isPresent()) {
                conv = existingInquiry.get();
                ensureCustomerParticipant(conv, user);
                return new CreateConversationOutcome(false, toConversationResponse(conv));
            }

            ChatConversation c = new ChatConversation();
            c.setType(ChatConversationType.INQUIRY);
            c.setProductId(req.getProductId());
            c.setCreatedByUserId(user.getId());
            conv = chatConversationRepository.save(c);

            ensureCustomerParticipant(conv, user);
            return new CreateConversationOutcome(true, toConversationResponse(conv));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "unsupported conversation type");
        }
    }

    public Page<ChatMessageResponse> getMessages(String username, Long conversationId, int page, int size) {
        User user = getUserByUsernameOr404(username);

        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a participant of this conversation");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatMessage> raw = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
        return raw.map(this::toMessageResponse);
    }

    @Transactional
    public ChatMessageResponse sendMessage(String username, Long conversationId, SendChatMessageRequest req) {
        User user = getUserByUsernameOr404(username);

        if (!chatConversationRepository.existsById(conversationId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found");
        }

        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a participant of this conversation");
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderUserId(user.getId());
        msg.setType(ChatMessageType.TEXT);
        msg.setContent(req.getContent().trim());
        ChatMessage saved = chatMessageRepository.save(msg);

        return toMessageResponse(saved);
    }
}

