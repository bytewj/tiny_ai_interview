package com.surenhao.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.entity.AiMessage;
import com.surenhao.backend.entity.SysUser;
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

    // --- 依赖注入 ---
    @Autowired
    @Qualifier("aiTaskExecutor")
    private ExecutorService executor; // 用于并行分析的线程池

    @Autowired
    private AiMessageMapper aiMessageMapper; // 用于流式对话存库

    // --- 工具类实例 ---
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";
    private final WebClient webClient = WebClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();


    // ==========================================
    // 业务 1: 并行执行 AI 分析任务 (CompletableFuture)
    // ==========================================
    @Override
    public AiAnalysisResult analyzeParallel() {
        long start = System.currentTimeMillis();
        SysUser currentUser = UserContext.get(); // 获取当前用户
        log.info("=== 开始并行执行 AI 分析任务 (用户: {}) ===", currentUser.getNickname());

        // 1. 开启异步任务 A
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() ->
                mockAiInference("岗位匹配度计算", 20), executor);

        // 2. 开启异步任务 B
        CompletableFuture<String> taskB = CompletableFuture.supplyAsync(() ->
                mockAiInference("面试表现打分", 12), executor);

        // 3. 开启异步任务 C
        CompletableFuture<String> taskC = CompletableFuture.supplyAsync(() ->
                mockAiInference("优缺点分析", 21), executor);

        // 等待所有任务完成
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


    // ==========================================
    // 业务 2: SSE 流式对话 (WebFlux)
    // ==========================================
    @Override
    public Flux<ServerSentEvent<String>> streamChat(String question) {
        // 1. 在主线程立刻获取用户 ID (防止进入 Reactor 线程后 ThreadLocal 丢失)
        SysUser user = UserContext.get();
        Long currentUserId = (user != null) ? user.getId() : 0L;

        // 2. 准备 StringBuilder 收集完整回答
        StringBuilder fullAnswerBuilder = new StringBuilder();

        // 3. 构造 Ollama 参数
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
                    // 1. 拼接到内存 (StringBuilder)
                    fullAnswerBuilder.append(content);
                    // 2. 发送给前端
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(counter.getAndIncrement()))
                            .event("message")
                            .data(content)
                            .build();
                })
                // ✅ 场景 A: 正常说完 -> 存库
                .doOnComplete(() -> {
                    log.info("AI 对话正常结束，入库...");
                    saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                })
                // 🔥🔥🔥 场景 B: 用户断开/取消 -> 也要存库！(必须加这个)
                .doOnCancel(() -> {
                    log.warn("检测到客户端断开连接，正在保存已生成内容...");
                    if (fullAnswerBuilder.length() > 0) {
                        // 把这半截话也存进数据库，至少用户回来能看到“半句话”，而不是记录丢了
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                })
                // ⚠️ 场景 C: 报错 -> 也可以选择存
                .doOnError(e -> {
                    log.error("流式生成异常", e);
                    if (fullAnswerBuilder.length() > 0) {
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                });
    }


    // ==========================================
    // 私有辅助方法 (Helper Methods)
    // ==========================================

    /**
     * 模拟耗时 AI 任务
     */
    private String mockAiInference(String taskName, int seconds) {
        SysUser user = UserContext.get(); // TTL 生效验证
        log.info(">>> [{}] 开始 (用户: {}, 线程: {})", taskName, user.getNickname(), Thread.currentThread().getName());
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return String.format("[%s] 完成 | 耗时%ds", taskName, seconds);
    }

    /**
     * 数据库持久化
     */
    private void saveToDb(Long userId, String question, String fullAnswer) {
        AiMessage message = new AiMessage();
        message.setUserId(userId);
        message.setUserQuestion(question);
        message.setAiAnswer(fullAnswer);
        message.setCreateTime(LocalDateTime.now());

        aiMessageMapper.insert(message);
        log.info("✅ 消息已保存 ID: {}", message.getId());
    }

    /**
     * 解析 Ollama JSON
     */
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