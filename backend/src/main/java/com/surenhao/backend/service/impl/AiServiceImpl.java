package com.surenhao.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.entity.AiMessage;
import com.surenhao.backend.entity.LoginUser;
import com.surenhao.backend.mapper.AiMessageMapper;
import com.surenhao.backend.service.AiService;
import com.surenhao.backend.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    @Qualifier("aiTaskExecutor")
    private ExecutorService executor;

    @Autowired
    private AiMessageMapper aiMessageMapper;

    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();


    @Override
    public AiAnalysisResult analyzeParallel() {
        long start = System.currentTimeMillis();

        // 2.【修改点】类型改为 LoginUser
        LoginUser currentUser = UserContext.get();
        log.info("=== 开始并行执行 AI 分析任务 (用户: {}) ===", currentUser.getNickname());

        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() ->
                mockAiInference("岗位匹配度计算", 20), executor);

        CompletableFuture<String> taskB = CompletableFuture.supplyAsync(() ->
                mockAiInference("面试表现打分", 12), executor);

        CompletableFuture<String> taskC = CompletableFuture.supplyAsync(() ->
                mockAiInference("优缺点分析", 21), executor);

        CompletableFuture.allOf(taskA, taskB, taskC).join();

        try {
            long totalTime = System.currentTimeMillis() - start;
            log.info("=== 并行任务结束，总耗时: {} ms ===", totalTime);
            return new AiAnalysisResult(taskA.get(), taskB.get(), taskC.get(), totalTime + " ms");
        } catch (Exception e) {
            log.error("AI 任务聚合失败", e);
            return new AiAnalysisResult("失败", "失败", "失败", "异常");
        }
    }

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String question) {
        // 3.【修改点】类型改为 LoginUser
        LoginUser user = UserContext.get();
        Long currentUserId = (user != null) ? user.getId() : 0L;

        StringBuilder fullAnswerBuilder = new StringBuilder();

        Map<String, Object> ollamaRequest = Map.of(
                "model", "qwen:1.8b",
                "prompt", "请简练回答：\n" + question,
                "stream", true
        );

        AtomicInteger counter = new AtomicInteger(1);

        return webClient.post()
                .uri(OLLAMA_API_URL)
                .bodyValue(ollamaRequest)
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::parseOllamaResponse)
                .filter(content -> !content.isEmpty())
                .map(content -> {
                    fullAnswerBuilder.append(content);
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(counter.getAndIncrement()))
                            .event("message")
                            .data(content)
                            .build();
                })
                .doOnComplete(() -> {
                    log.info("AI 对话正常结束，入库...");
                    saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                })
                .doOnCancel(() -> {
                    log.warn("检测到客户端断开连接，正在保存已生成内容...");
                    if (fullAnswerBuilder.length() > 0) {
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                })
                .doOnError(e -> {
                    log.error("流式生成异常", e);
                    if (fullAnswerBuilder.length() > 0) {
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                });
    }

    private String mockAiInference(String taskName, int seconds) {
        // 4.【修改点】类型改为 LoginUser
        LoginUser user = UserContext.get();
        log.info(">>> [{}] 开始 (用户: {}, 线程: {})", taskName, user.getNickname(), Thread.currentThread().getName());
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return String.format("[%s] 完成 | 耗时%ds", taskName, seconds);
    }

    private void saveToDb(Long userId, String question, String fullAnswer) {
        AiMessage message = new AiMessage();
        message.setUserId(userId);
        message.setUserQuestion(question);
        message.setAiAnswer(fullAnswer);
        message.setCreateTime(LocalDateTime.now());

        aiMessageMapper.insert(message);
        log.info("✅ 消息已保存 ID: {}", message.getId());
    }

    private String parseOllamaResponse(String jsonLine) {
        try {
            JsonNode node = objectMapper.readTree(jsonLine);
            if (node.has("response")) {
                return node.get("response").asText();
            }
        } catch (Exception ignored) {}
        return "";
    }
}