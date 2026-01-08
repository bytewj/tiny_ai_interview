package com.surenhao.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.surenhao.backend.entity.ChatMessage;
import com.surenhao.backend.entity.UserOnlineStatus;
import com.surenhao.backend.mapper.ChatMessageMapper;
import com.surenhao.backend.mapper.UserOnlineStatusMapper;
import com.surenhao.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class ChatServiceImpl extends ServiceImpl<ChatMessageMapper, ChatMessage> implements ChatService {

    @Autowired
    private UserOnlineStatusMapper statusMapper;

    @Override
    @Transactional(rollbackFor = Exception.class) // 建议加上事务，保证数据一致性
    public void userOnline(Long userId, String sessionId) {
        UserOnlineStatus status = statusMapper.selectOne(new LambdaQueryWrapper<UserOnlineStatus>()
                .eq(UserOnlineStatus::getUserId, userId));

        if (status == null) {
            status = new UserOnlineStatus();
            status.setUserId(userId);
        }
        status.setOnlineStatus(1); // 1在线
        status.setWebsocketSessionId(sessionId);
        status.setLastActiveTime(new Date());

        if (status.getId() == null) {
            statusMapper.insert(status);
        } else {
            statusMapper.updateById(status);
        }
    }

    @Override
    public void userOffline(String sessionId) {
        UserOnlineStatus status = statusMapper.selectOne(new LambdaQueryWrapper<UserOnlineStatus>()
                .eq(UserOnlineStatus::getWebsocketSessionId, sessionId));
        if (status != null) {
            status.setOnlineStatus(0); // 0离线
            status.setWebsocketSessionId(null);
            statusMapper.updateById(status);
        }
    }

    @Override
    public void saveMessage(Long senderId, Integer senderType, Long receiverId, Integer receiverType, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setSenderId(senderId);
        msg.setSenderType(senderType);     // ✅ 新增
        msg.setReceiverId(receiverId);
        msg.setReceiverType(receiverType); // ✅ 新增
        msg.setContent(content);
        msg.setCreateTime(new Date());

        // 使用 ServiceImpl 提供的 save 方法
        this.save(msg);
    }
}