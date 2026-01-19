package com.surenhao.backend.entity;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiChatRequest {

    // 自动校验：不能为 null 且去空格后长度必须大于 0
    @NotBlank(message = "提问内容不能为空")
    private String message;
}