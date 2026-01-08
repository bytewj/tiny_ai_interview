package com.surenhao.backend.websocket;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.surenhao.backend.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@ServerEndpoint("/api/chat/{userType}/{userId}")
public class ChatEndpoint {

    // 静态变量注入 Service
    private static ChatService chatService;

    // 2. 内存会话管理：Key 变成了 "身份_ID" (例如 "1_1001")
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();

    @Autowired
    public void setChatService(ChatService chatService) {
        ChatEndpoint.chatService = chatService;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        try {
            // 3. 生成复合键 (Composite Key)
            String key = userType + "_" + userId;
            sessions.put(key, session);

            log.info("用户上线: 身份={} ID={} (Key={})", userType, userId, key);
        } catch (Exception e) {
            log.error("连接异常", e);
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JSONObject json = JSON.parseObject(message);

            // 获取消息里的完整信息
            Integer senderType = json.getInteger("senderType");
            Long senderId = json.getLong("senderId");
            Integer receiverType = json.getInteger("receiverType");
            Long receiverId = json.getLong("receiverId");
            String content = json.getString("content");

            // 4. 存入数据库 (持久化) - 记得修改你的 Service 支持这两个新字段
            chatService.saveMessage(senderId, senderType, receiverId, receiverType, content);
            log.info("收到消息: {} (Type {}) -> {} (Type {})", senderId, senderType, receiverId, receiverType);

            // 5. 核心路由逻辑：计算接收者的 Key
            String receiverKey = receiverType + "_" + receiverId;
            Session receiverSession = sessions.get(receiverKey);

            if (receiverSession != null && receiverSession.isOpen()) {
                // 实时推送
                receiverSession.getBasicRemote().sendText(message);
            } else {
                log.info("接收者 {} 不在线，消息已存库", receiverKey);
            }

        } catch (Exception e) {
            log.error("消息处理失败", e);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("userType") String userType, @PathParam("userId") String userId) {
        String key = userType + "_" + userId;
        sessions.remove(key);
        log.info("用户下线: {}", key);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WS错误", error);
    }
}