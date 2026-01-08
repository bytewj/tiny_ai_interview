package com.surenhao.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.surenhao.backend.entity.ChatMessage;
import com.surenhao.backend.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin // 允许前端跨域调用
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 拉取历史消息接口
    @GetMapping("/history")
    public List<ChatMessage> getHistory(
            @RequestParam Long myId, @RequestParam Integer myType,
            @RequestParam Long toId, @RequestParam Integer toType) {

        // MyBatis-Plus 查询逻辑：
        // 查找 (我是发送者 AND 他是接收者) OR (我是接收者 AND 他是发送者)
        LambdaQueryWrapper<ChatMessage> query = new LambdaQueryWrapper<>();

        query.and(wrapper -> wrapper
                // 情况1：我发给他的
                .eq(ChatMessage::getSenderId, myId)
                .eq(ChatMessage::getSenderType, myType)
                .eq(ChatMessage::getReceiverId, toId)
                .eq(ChatMessage::getReceiverType, toType)
        ).or(wrapper -> wrapper
                // 情况2：他发给我的
                .eq(ChatMessage::getSenderId, toId)
                .eq(ChatMessage::getSenderType, toType)
                .eq(ChatMessage::getReceiverId, myId)
                .eq(ChatMessage::getReceiverType, myType)
        );

        // 按时间正序排列（旧消息在前，新消息在后）
        query.orderByAsc(ChatMessage::getCreateTime);

        return chatService.list(query);
    }
}