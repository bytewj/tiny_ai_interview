package com.surenhao.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_ai_message")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String userQuestion;
    private String aiAnswer;
    private LocalDateTime createTime;
}