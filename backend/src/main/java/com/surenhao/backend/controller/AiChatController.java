package com.surenhao.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surenhao.backend.annotation.Public;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiChatController {

    // Mac 本地 Ollama 默认地址
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    // WebClient 是 Spring WebFlux 提供的非阻塞 HTTP 客户端
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * AI 对话接口 (SSE 流式响应)
     * 1. @Public: 免登录直接用 (方便演示)
     * 2. produces: 声明返回的是流 (text/event-stream)
     */
    @Public
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody Map<String, String> params) {
        String prompt = params.get("message");

        // 构造 Ollama 需要的参数
        // model: 必须和你终端里 ollama list 出来的名字一致
        Map<String, Object> requestBody = Map.of(
                "model", "qwen:1.8b",
                "prompt", "你是一个资深的职业规划师，请用简练专业的语气回答：\n" + prompt,
                "stream", true // 开启流式模式
        );

        System.out.println("AI 请求: " + prompt);

        // 发起请求并处理流
        return webClient.post()
                .uri(OLLAMA_API_URL)
                .bodyValue(requestBody)
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .bodyToFlux(String.class)
                .map(jsonLine -> {
                    try {
                        // 解析每一行 JSON: {"response": "我", "done": false ...}
                        JsonNode node = objectMapper.readTree(jsonLine);
                        if (node.has("response")) {
                            return node.get("response").asText();
                        }
                        return "";
                    } catch (Exception e) {
                        return "";
                    }
                });
    }
}