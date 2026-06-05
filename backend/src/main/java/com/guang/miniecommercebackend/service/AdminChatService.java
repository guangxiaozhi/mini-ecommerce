//把 Admin 聊天相关的业务逻辑写在这里（列表、接单、看消息、客服发消息）
package com.guang.miniecommercebackend.service;

import com.guang.miniecommercebackend.dto.ChatConversationResponse;
import com.guang.miniecommercebackend.dto.ChatMessageResponse;
import com.guang.miniecommercebackend.dto.SendChatMessageRequest;
import com.guang.miniecommercebackend.entity.*;
import com.guang.miniecommercebackend.repository.ChatConversationRepository;
import com.guang.miniecommercebackend.repository.ChatMessageRepository;
import com.guang.miniecommercebackend.repository.ChatParticipantRepository;
import com.guang.miniecommercebackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guang.miniecommercebackend.entity.ChatMessageType;


@Service
public class AdminChatService {
    private final ChatConversationRepository chatConversationRepository;
    private final ChatParticipantRepository chatParticipantRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    //    接单后可选插一条 SYSTEM 消息，发送者仍是 bot 用户
    @Value("${chat.bot.username:chat_bot}")
    private String chatBotUsername;
    private static final String AGENT_JOINED_SYSTEM_MESSAGE =
            "An agent has joined the chat.";

    private static final String CONVERSATION_CLOSED_SYSTEM_MESSAGE =
            "This conversation has been closed.";

    public AdminChatService(ChatConversationRepository chatConversationRepository,
                            ChatParticipantRepository chatParticipantRepository,
                            ChatMessageRepository chatMessageRepository,
                            UserRepository userRepository){
        this.chatConversationRepository = chatConversationRepository;
        this.chatParticipantRepository = chatParticipantRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

//    Controller 传进来的是 JWT 里的 username，这里换成 User 实体（需要 user.getId()）
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

    private User getBotUserOr404() {
        return userRepository.findByUsername(chatBotUsername)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "chat bot user not found"));
    }

    private void insertSystemMessage(ChatConversation conv, String content) {
        User bot = getBotUserOr404();
        ChatMessage sys = new ChatMessage();
        sys.setConversationId(conv.getId());
        sys.setSenderUserId(bot.getId());
        sys.setType(ChatMessageType.SYSTEM);
        sys.setContent(content);
        chatMessageRepository.save(sys);
    }

//    权限判断
    private Authentication currentAuth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    private boolean isSuperAdmin() {
        return currentAuth().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
    private boolean hasAuthority(String code) {
        return currentAuth().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(code::equals);
    }
    private void assertCanViewType(ChatConversationType type) {
        if (isSuperAdmin()) return;
        if (type == ChatConversationType.ORDER) {
            if (!hasAuthority("CHAT_ORDER_VIEW_ASSIGNED")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission to view order chats");
            }
        } else if (type == ChatConversationType.INQUIRY) {
            if (!hasAuthority("CHAT_INQUIRY_VIEW_ASSIGNED")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission to view inquiry chats");
            }
        }
    }
    private void assertCanReplyType(ChatConversationType type) {
        if (isSuperAdmin()) return;
        if (type == ChatConversationType.ORDER) {
            if (!hasAuthority("CHAT_ORDER_REPLY")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission to reply to order chats");
            }
        } else if (type == ChatConversationType.INQUIRY) {
            if (!hasAuthority("CHAT_INQUIRY_REPLY")) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "no permission to reply to inquiry chats");
            }
        }
    }
    private void assertCanCloseConversation() {
        if (isSuperAdmin()) return;
        if (!hasAuthority("CHAT_CONVERSATION_CLOSE")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "no permission to close conversations");
        }
    }

//    确保客服是参与者，接单时把客服写进会话参与者表，接单时在 t_chat_participant 里写一条 HUMAN_AGENT 记录。
    private void ensureHumanAgentParticipant(ChatConversation conv, User agent) {
        if (chatParticipantRepository.existsByConversationIdAndUserIdAndRole(
                conv.getId(), agent.getId(), ChatParticipantRole.HUMAN_AGENT)) {
            return;
        }
        if (chatParticipantRepository.existsByConversationIdAndUserId(conv.getId(), agent.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "user already participates in this conversation with another role");
        }
        ChatParticipant p = new ChatParticipant();
        p.setConversationId(conv.getId());
        p.setUserId(agent.getId());
        p.setRole(ChatParticipantRole.HUMAN_AGENT);
        chatParticipantRepository.save(p);
    }

//    按条件从数据库查出会话列表，做权限校验，转成 DTO 分页返回。
    @Transactional(readOnly = true)
    public Page<ChatConversationResponse> listAdminConversations(
            String username,
            ChatConversationStatus status,
            ChatConversationType type,
            boolean assignedToMe,
            int page,
            int size) {
        User agent = getUserByUsernameOr404(username); //当前登录客服（从 JWT 来）
        Pageable pageable = PageRequest.of(page, size);
        ChatConversationStatus effectiveStatus = status != null ? status : ChatConversationStatus.WAITING_HUMAN; //要查哪种状态；不传默认当 WAITING_HUMAN（待接单）

        if (assignedToMe == true) { // 我负责的单：进行中 ASSIGNED，或已关闭 CLOSED
            ChatConversationStatus mineStatus =
                    status == ChatConversationStatus.CLOSED
                            ? ChatConversationStatus.CLOSED
                            : ChatConversationStatus.ASSIGNED;
            Page<ChatConversation> pageChat;
            if (type != null) {
                assertCanViewType(type);
                pageChat = chatConversationRepository
                        .findByAssignedAgentUserIdAndStatusAndTypeOrderByCreatedAtDesc(
                                agent.getId(), mineStatus, type, pageable);
            } else {
                if (!isSuperAdmin()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
                }
                pageChat = chatConversationRepository
                        .findByAssignedAgentUserIdAndStatusOrderByCreatedAtDesc(
                                agent.getId(), mineStatus, pageable);
            }
            return pageChat.map(this::toConversationResponse);
        } else { // 待接单 / 按 status的列表
            if (type != null) {
                assertCanViewType(type);
                Page<ChatConversation> pageChat = chatConversationRepository
                        .findByStatusAndTypeOrderByCreatedAtDesc(effectiveStatus, type, pageable);
                return pageChat.map(this::toConversationResponse);
            }
            if (!isSuperAdmin()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type is required");
            }
            Page<ChatConversation> pageChat = chatConversationRepository
                    .findByStatusOrderByCreatedAtDesc(effectiveStatus, pageable);
            return pageChat.map(this::toConversationResponse);
        }


    }

    @Transactional
    public ChatConversationResponse assignConversation(String username, Long conversationId) {
        User agent = getUserByUsernameOr404(username); //当前登录客服（从 JWT 来）
        ChatConversation conv = chatConversationRepository.findById(conversationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));
        assertCanReplyType(conv.getType());
        if (conv.getStatus() != ChatConversationStatus.WAITING_HUMAN){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"conversation is not waiting for human");
        }
        if (conv.getAssignedAgentUserId() != null && conv.getAssignedAgentUserId() != agent.getId()){
            throw new ResponseStatusException(HttpStatus.CONFLICT,"conversation already assigned");
        }
        ensureHumanAgentParticipant(conv, agent); // 参与者：同一个人，角色 HUMAN_AGENT
        conv.setAssignedAgentUserId(agent.getId());  // 会话：负责人是谁
        conv.setStatus(ChatConversationStatus.ASSIGNED);
        chatConversationRepository.save(conv);
        insertSystemMessage(conv, AGENT_JOINED_SYSTEM_MESSAGE);
        return toConversationResponse(conv);
    }

//    客服（或 super admin）查看某个会话里的历史消息，分页返回。
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessagesForAdmin(
            String username,
            Long conversationId,
            int page,
            int size) {
        User agent = getUserByUsernameOr404(username);
        ChatConversation conv = chatConversationRepository.findById(conversationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        assertCanViewType(conv.getType());  //检查 VIEW 权限

        if (!isSuperAdmin()) {  //Super admin（ROLE_ADMIN）跳过下面限制，方便调试
            if (conv.getStatus() != ChatConversationStatus.ASSIGNED
                    && conv.getStatus() != ChatConversationStatus.CLOSED) {  //还在 WAITING_HUMAN（待接单）时，普通客服不能先看聊天记录
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to view this conversation");
            }
            if (!agent.getId().equals(conv.getAssignedAgentUserId())) {  //必须是接单的那位客服（会话上的负责人）
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to view this conversation");
            }
            if (!chatParticipantRepository.existsByConversationIdAndUserIdAndRole(
                    conversationId, agent.getId(), ChatParticipantRole.HUMAN_AGENT)) { //参与者表里也必须是 HUMAN_AGENT
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to view this conversation");
            }
        }
        Pageable pageable = PageRequest.of(page, size);

//        查消息并转成 DTO
        Page<ChatMessage> raw = chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);  //按 conversation_id 查 t_chat_message，时间从早到晚（和用户聊天室一致）

        return raw.map(this::toMessageResponse);// 每条 ChatMessage → ChatMessageResponse（id、content、type、senderUserId…）,方法结束，把分页结果交给 Controller

    }

//    客服消息就是普通 TEXT；没有 botMayReply、没有转人工分支。
    @Transactional
    public ChatMessageResponse sendMessageAsAgent(
            String username,
            Long conversationId,
            SendChatMessageRequest req) {
        User agent = getUserByUsernameOr404(username);
        ChatConversation conv = chatConversationRepository.findById(conversationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        assertCanReplyType(conv.getType());

        if (!isSuperAdmin()) {
            if (conv.getStatus() != ChatConversationStatus.ASSIGNED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to send in this conversation");
            }
            if (!agent.getId().equals(conv.getAssignedAgentUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to send in this conversation");
            }
            if (!chatParticipantRepository.existsByConversationIdAndUserIdAndRole(
                    conversationId, agent.getId(), ChatParticipantRole.HUMAN_AGENT)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to send in this conversation");
            }
        }

        ChatMessage msg = new ChatMessage();
        msg.setConversationId(conv.getId());
        msg.setSenderUserId(agent.getId());
        msg.setType(ChatMessageType.TEXT);
        msg.setContent(req.getContent().trim());
        ChatMessage saved = chatMessageRepository.save(msg);
        return toMessageResponse(saved);
    }

    @Transactional
    public ChatConversationResponse closeConversation(String username, Long conversationId) {
        User agent = getUserByUsernameOr404(username);
        ChatConversation conv = chatConversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "conversation not found"));

        assertCanCloseConversation();

        if (conv.getStatus() == ChatConversationStatus.CLOSED) {
            return toConversationResponse(conv);  // 已关闭，幂等返回
        }

        if (conv.getStatus() != ChatConversationStatus.ASSIGNED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "only assigned conversations can be closed");
        }

        if (!isSuperAdmin()) {
            if (!agent.getId().equals(conv.getAssignedAgentUserId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to close this conversation");
            }
            if (!chatParticipantRepository.existsByConversationIdAndUserIdAndRole(
                    conversationId, agent.getId(), ChatParticipantRole.HUMAN_AGENT)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "not allowed to close this conversation");
            }
        }

        conv.setStatus(ChatConversationStatus.CLOSED);
        chatConversationRepository.save(conv);
        insertSystemMessage(conv, CONVERSATION_CLOSED_SYSTEM_MESSAGE);
        return toConversationResponse(conv);
    }
}

