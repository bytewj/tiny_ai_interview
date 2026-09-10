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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    /**
     * 注入自定义的 TTL 线程池 (AiThreadPoolConfig 中定义的)
     * 作用：处理并发任务，且能自动透传 ThreadLocal 上下文
     */
    @Autowired
    @Qualifier("aiTaskExecutor")
    private ExecutorService executor;

    @Autowired
    private AiMessageMapper aiMessageMapper;

    // Ollama 本地接口地址 (建议后续放入 application.yml 配置)
    private static final String OLLAMA_API_URL = "http://localhost:11434/api/generate";

    // WebClient 实例 (Spring WebFlux)
    private final WebClient webClient = WebClient.create();

    // Jackson 序列化工具
    private final ObjectMapper objectMapper = new ObjectMapper();


    /**
     * 【核心功能】并行执行三个独立的 AI 分析服务
     * 特性：并发加速 + 超时控制 + 异常兜底
     */
    @Override
    public CompletableFuture<AiAnalysisResult> analyzeParallel() {
        long start = System.currentTimeMillis();

        // 1. 【主线程】获取当前用户
        // 虽然 TTL 线程池可以自动透传，但显式获取对于代码可读性更好
        final LoginUser currentUser = UserContext.get();
        String nickname = (currentUser != null) ? currentUser.getNickname() : "未知用户";
        log.info("=== [AI主线程] 开始并行调度三个 AI 服务 (当前用户: {}) ===", nickname);

        // 2. 【异步】启动三个并行任务

        // 任务 A：岗位匹配度 (限时 3秒)
        CompletableFuture<String> matchFuture = CompletableFuture.supplyAsync(() ->
                        callMatchDegreeService(currentUser), executor)
                .orTimeout(3, TimeUnit.SECONDS) // JDK 9+ 新特性：超时抛异常
                .exceptionally(e -> {
                    log.error("[任务A-匹配度] 执行失败或超时: {}", e.getMessage());
                    return "计算超时/服务不可用"; // 降级返回值
                });

        // 任务 B：面试表现打分 (限时 5秒)
        CompletableFuture<String> scoreFuture = CompletableFuture.supplyAsync(() ->
                        callInterviewScoringService(currentUser), executor)
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("[任务B-面试打分] 执行失败或超时: {}", e.getMessage());
                    return "评分暂时不可用";
                });

        // 任务 C：优缺点分析 (限时 3秒)
        CompletableFuture<String> analysisFuture = CompletableFuture.supplyAsync(() ->
                        callProsConsAnalysisService(currentUser), executor)
                .orTimeout(3, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    log.error("[任务C-优缺点] 执行失败或超时: {}", e.getMessage());
                    return "无法分析优缺点";
                });

        // 3. 【编排】等待所有任务完成 (join 此时是安全的，因为有 exceptionally 兜底)
        return CompletableFuture.allOf(matchFuture, scoreFuture, analysisFuture)
                .thenApply(v -> {
                    try {
                        String matchResult = matchFuture.join();
                        String scoreResult = scoreFuture.join();
                        String analysisResult = analysisFuture.join();

                        long totalTime = System.currentTimeMillis() - start;
                        log.info("=== [AI聚合] 三项服务全部完成，总耗时: {} ms ===", totalTime);

                        return new AiAnalysisResult(matchResult, scoreResult, analysisResult, totalTime + " ms");
                    } catch (Exception e) {
                        log.error("AI 结果聚合未知异常", e);
                        // 最后的兜底防止崩前端
                        return new AiAnalysisResult("Error", "Error", "Error", "0 ms");
                    }
                });
    }

    // ================== 模拟业务调用逻辑 (实际开发中替换为 HTTP 请求) ==================

    private String callMatchDegreeService(LoginUser user) {
        log.info(">>> [任务A] 正在计算匹配度... 线程: {}", Thread.currentThread().getName());
        // 模拟业务耗时
        sleep(1000);
        return "98% (极高适配)";
    }

    private String callInterviewScoringService(LoginUser user) {
        log.info(">>> [任务B] 正在进行打分... 线程: {}", Thread.currentThread().getName());
        // 模拟较长的业务耗时
        sleep(2000);
        return "85分 (基础扎实，逻辑清晰)";
    }

    private String callProsConsAnalysisService(LoginUser user) {
        log.info(">>> [任务C] 正在分析优缺点... 线程: {}", Thread.currentThread().getName());
        sleep(1500);
        return "优点：并发理解深刻；缺点：项目经验略少";
    }

    /**
     * 辅助方法：模拟耗时，捕获中断异常
     */
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================== 流式对话功能 (Server-Sent Events) ==================

    @Override
    public Flux<ServerSentEvent<String>> streamChat(String question) {
        // 1. 获取用户信息 (用于后续入库)
        LoginUser user = UserContext.get();
        Long currentUserId = (user != null) ? user.getId() : 0L;

        // 2. 用于拼接完整回复的容器
        StringBuilder fullAnswerBuilder = new StringBuilder();

        // 3. 构建 Ollama 请求参数
        Map<String, Object> ollamaRequest = Map.of(
                "model", "qwen:1.8b", // 模型名称需与本地 Ollama 一致
                "prompt", "请简练回答：\n" + question,
                "stream", true
        );

        // 4. 计数器 (用于 SSE ID)
        AtomicInteger counter = new AtomicInteger(1);

        // 5. 发起流式请求
        return webClient.post()
                .uri(OLLAMA_API_URL)
                .bodyValue(ollamaRequest)
                .accept(MediaType.APPLICATION_NDJSON)
                .retrieve()
                .bodyToFlux(String.class)
                .map(this::parseOllamaResponse) // 解析 JSON 提取 content
                .filter(content -> !content.isEmpty()) // 过滤空帧
                .map(content -> {
                    // 拼接完整答案
                    fullAnswerBuilder.append(content);
                    // 构建 SSE 数据包
                    return ServerSentEvent.<String>builder()
                            .id(String.valueOf(counter.getAndIncrement()))
                            .event("message")
                            .data(content)
                            .build();
                })
                // --- 异步回调处理 (入库逻辑) ---
                .doOnComplete(() -> {
                    log.info("AI 对话正常结束，准备入库...");
                    saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                })
                .doOnCancel(() -> {
                    log.warn("检测到客户端断开连接 (Cancel)，保存已生成内容...");
                    if (fullAnswerBuilder.length() > 0) {
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                })
                .doOnError(e -> {
                    log.error("流式生成发生异常", e);
                    // 即使报错，也尝试保存已生成的部分
                    if (fullAnswerBuilder.length() > 0) {
                        saveToDb(currentUserId, question, fullAnswerBuilder.toString());
                    }
                });
    }

    /**
     * 辅助方法：将对话记录保存到数据库
     */
    private void saveToDb(Long userId, String question, String fullAnswer) {
        try {
            AiMessage message = new AiMessage();
            message.setUserId(userId);
            message.setUserQuestion(question);
            message.setAiAnswer(fullAnswer);
            message.setCreateTime(LocalDateTime.now());

            aiMessageMapper.insert(message);
            log.info("✅ 会话记录已保存 ID: {}", message.getId());
        } catch (Exception e) {
            log.error("❌ 会话记录入库失败", e);
        }
    }

    /**
     * 辅助方法：解析 Ollama 返回的 JSON 每一行
     * 格式示例: { "model": "qwen", "created_at": "...", "response": "你好", "done": false }
     */
    private String parseOllamaResponse(String jsonLine) {
        try {
            JsonNode node = objectMapper.readTree(jsonLine);
            if (node.has("response")) {
                return node.get("response").asText();
            }
        } catch (Exception e) {
            // 解析失败忽略当前帧
        }
        return "";
    }
}