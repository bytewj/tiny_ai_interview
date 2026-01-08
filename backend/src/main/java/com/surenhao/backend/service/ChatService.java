package com.surenhao.backend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.surenhao.backend.entity.ChatMessage;

/**
 * 聊天服务接口
 * 继承 IService<ChatMessage> 可以直接获得 MyBatis-Plus 提供的基础 CRUD 能力
 */
public interface ChatService extends IService<ChatMessage> {

    /**
     * 用户上线
     */
    void userOnline(Long userId, String sessionId);

    /**
     * 用户下线
     */
    void userOffline(String sessionId);

    /**
     * 保存消息 (已更新参数，支持身份类型)
     */
    void saveMessage(Long senderId, Integer senderType, Long receiverId, Integer receiverType, String content);
}