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


import org.springframework.beans.factory.annotation.Value;
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
    private final ChatMessagePublisher chatMessagePublisher;

    @Value("${chat.bot.username:chat_bot}")
    private String chatBotUsername;

    private static final String TRANSFER_TO_HUMAN_SYSTEM_MESSAGE =
            "Your request has been sent to our support team. Please wait for an agent.";

    private static final String CONVERSATION_REOPENED_SYSTEM_MESSAGE =
            "This conversation has been reopened. You are now chatting with our virtual assistant (not a live agent). "
                    + "To speak with a human agent, tap \"Speak to an agent\" above or type \"Speak to an Agent\" .";

    private User getBotUserOr404() {
        return userRepository.findByUsername(chatBotUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "chat bot user not found"));
    }

    public ChatService(ChatConversationRepository chatConversationRepository,
                       ChatParticipantRepository chatParticipantRepository,
                       ChatMessageRepository chatMessageRepository,
                       UserRepository userRepository,
                       OrderRepository orderRepository,
                       ProductRepository productRepository,
                       ChatMessagePublisher chatMessagePublisher
                       ){
        this.chatConversationRepository = chatConversationRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.chatMessagePublisher = chatMessagePublisher;
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
        r.setStatus(c.getStatus() != null ? c.getStatus().name() : ChatConversationStatus.BOT.name());
        r.setAssignedAgentUserId(c.getAssignedAgentUserId());
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

    private void ensureBotParticipant(ChatConversation conv) {
        User bot = getBotUserOr404();
        if (!chatParticipantRepository.existsByConversationIdAndUserId(conv.getId(), bot.getId())) {
            ChatParticipant p = new ChatParticipant();
            p.setConversationId(conv.getId());
            p.setUserId(bot.getId());
            p.setRole(ChatParticipantRole.BOT);
            chatParticipantRepository.save(p);
        }
    }

    private void seedBotWelcomeMessage(ChatConversation conv) {
        User bot = getBotUserOr404();
        ChatMessage welcome = new ChatMessage();
        welcome.setConversationId(conv.getId());
        welcome.setSenderUserId(bot.getId());
        welcome.setType(ChatMessageType.TEXT);
        welcome.setContent("Hi, this is our virtual assistant. How can I help you today? \n If you need human support, please reply “Speak to an Agent”.");
        chatMessageRepository.save(welcome);
    }

    private void insertSystemMessage(ChatConversation conv, String content) {
        User bot = getBotUserOr404();
        ChatMessage sys = new ChatMessage();
        sys.setConversationId(conv.getId());
        sys.setSenderUserId(bot.getId());
        sys.setType(ChatMessageType.SYSTEM);
        sys.setContent(content);
        ChatMessage saved = chatMessageRepository.save(sys);
        chatMessagePublisher.publishToConversation(conv.getId(), toMessageResponse(saved));
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
                ensureBotParticipant(conv);
                return new CreateConversationOutcome(false, toConversationResponse(conv));
            }

            ChatConversation c = new ChatConversation();
            c.setType(ChatConversationType.ORDER);
            c.setOrderId(order.getId());
            c.setCreatedByUserId(user.getId());
            conv = chatConversationRepository.save(c);

            ensureCustomerParticipant(conv, user);
            ensureBotParticipant(conv);
            seedBotWelcomeMessage(conv);
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
                ensureBotParticipant(conv);
                return new CreateConversationOutcome(false, toConversationResponse(conv));
            }

            ChatConversation c = new ChatConversation();
            c.setType(ChatConversationType.INQUIRY);
            c.setProductId(req.getProductId());
            c.setCreatedByUserId(user.getId());
            conv = chatConversationRepository.save(c);

            ensureCustomerParticipant(conv, user);
            ensureBotParticipant(conv);
            seedBotWelcomeMessage(conv);
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

        ChatConversation conv = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a participant of this conversation");
        }

        if (conv.getStatus() == ChatConversationStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation is closed");
        }

        ChatConversationStatus st = conv.getStatus() != null ? conv.getStatus() : ChatConversationStatus.BOT;

        boolean botMayReply = st == ChatConversationStatus.BOT
                && !chatParticipantRepository.existsByConversationIdAndRole(
                conversationId, ChatParticipantRole.HUMAN_AGENT);

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conversationId);
        msg.setSenderUserId(user.getId());
        msg.setType(ChatMessageType.TEXT);
        msg.setContent(req.getContent().trim());
        ChatMessage saved = chatMessageRepository.save(msg);     // 1. 先存库（和以前一样）

        ChatMessageResponse userMsg = toMessageResponse(saved);
        chatMessagePublisher.publishToConversation(conversationId, userMsg);  // 2. 再推送

        String text = req.getContent().trim();

        if ("Speak to an Agent".equalsIgnoreCase(text) || "转人工".equals(text)) {
            if (conv.getStatus() == null || conv.getStatus() == ChatConversationStatus.BOT) {
                conv.setStatus(ChatConversationStatus.WAITING_HUMAN);
                chatConversationRepository.save(conv);
                insertSystemMessage(conv, TRANSFER_TO_HUMAN_SYSTEM_MESSAGE);
            }
            return userMsg;
        }

        if (botMayReply) {
            User bot = getBotUserOr404();
            ChatMessage reply = new ChatMessage();
            reply.setConversationId(conversationId);
            reply.setSenderUserId(bot.getId());
            reply.setType(ChatMessageType.TEXT);
            reply.setContent("We have received your message. Our customer service assistant will get back to you as soon as possible.\n");
            ChatMessage savedReply = chatMessageRepository.save(reply);
            chatMessagePublisher.publishToConversation(conversationId, toMessageResponse(savedReply));
        }

        return userMsg;
    }

    @Transactional
    public ChatConversationResponse transferToHuman(String username, Long conversationId) {
        User user = getUserByUsernameOr404(username);

        ChatConversation conv = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a participant of this conversation");
        }

        if (conv.getStatus() == ChatConversationStatus.WAITING_HUMAN
                || conv.getStatus() == ChatConversationStatus.ASSIGNED) {
            return toConversationResponse(conv);
        }

        if (conv.getStatus() == ChatConversationStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "conversation is closed");
        }

        conv.setStatus(ChatConversationStatus.WAITING_HUMAN);
        chatConversationRepository.save(conv);

        insertSystemMessage(conv, TRANSFER_TO_HUMAN_SYSTEM_MESSAGE);

        return toConversationResponse(conv);
    }

    @Transactional
    public ChatConversationResponse reopenConversation(String username, Long conversationId) {
        User user = getUserByUsernameOr404(username);

        ChatConversation conv = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        if (!chatParticipantRepository.existsByConversationIdAndUserId(conversationId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "not a participant of this conversation");
        }

        if (conv.getStatus() != ChatConversationStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "only closed conversations can be reopened");
        }

        conv.setStatus(ChatConversationStatus.BOT);
        conv.setAssignedAgentUserId(null);
        chatConversationRepository.save(conv);

        chatParticipantRepository.deleteByConversationIdAndRole(
                conversationId, ChatParticipantRole.HUMAN_AGENT);

        insertSystemMessage(conv, CONVERSATION_REOPENED_SYSTEM_MESSAGE);

        return toConversationResponse(conv);
    }

    public Page<ChatConversationResponse> listMyConversations(String username, int page, int size) {
        User user = getUserByUsernameOr404(username);

        Pageable pageable = PageRequest.of(page, size);
        Page<ChatParticipant> participantPage =
                chatParticipantRepository.findByUserIdOrderByJoinedAtDesc(user.getId(), pageable);

        return participantPage.map(p -> {
            ChatConversation conv = chatConversationRepository.findById(p.getConversationId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "conversation missing"));
            return toConversationResponse(conv);
        });
    }
}

