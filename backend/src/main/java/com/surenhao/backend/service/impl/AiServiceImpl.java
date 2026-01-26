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


    /**
     * 并行执行三个独立的 AI 服务
     */
    @Override
    public CompletableFuture<AiAnalysisResult> analyzeParallel() {
        long start = System.currentTimeMillis();

        // 1. 【主线程】提取上下文（必须在这里取，否则子线程拿不到）
        final LoginUser currentUser = UserContext.get();
        String nickname = (currentUser != null) ? currentUser.getNickname() : "未知用户";
        log.info("=== [主线程] 开始并行调度三个 AI 服务 (用户: {}) ===", nickname);

        // 2. 【异步】服务一：岗位匹配度计算
        CompletableFuture<String> matchFuture = CompletableFuture.supplyAsync(() ->
                callMatchDegreeService(currentUser), executor);

        // 3. 【异步】服务二：面试表现打分
        CompletableFuture<String> scoreFuture = CompletableFuture.supplyAsync(() ->
                callInterviewScoringService(currentUser), executor);

        // 4. 【异步】服务三：优缺点分析
        CompletableFuture<String> analysisFuture = CompletableFuture.supplyAsync(() ->
                callProsConsAnalysisService(currentUser), executor);

        // 5. 【编排】等待三个任务全部完成，组装结果
        return CompletableFuture.allOf(matchFuture, scoreFuture, analysisFuture)
                .thenApply(v -> {
                    try {
                        // 因为 allOf 保证了都完成，这里 get() 是瞬时的，不会阻塞
                        String matchResult = matchFuture.get();
                        String scoreResult = scoreFuture.get();
                        String analysisResult = analysisFuture.get();

                        long totalTime = System.currentTimeMillis() - start;
                        log.info("=== [异步回调] 三项服务全部聚合完成，总耗时: {} ms ===", totalTime);

                        return new AiAnalysisResult(matchResult, scoreResult, analysisResult, totalTime + " ms");
                    } catch (Exception e) {
                        log.error("AI 服务聚合结果失败", e);
                        // 兜底策略：返回错误提示，防止前端崩掉
                        return new AiAnalysisResult("计算失败", "评分失败", "分析失败", "0 ms");
                    }
                });
    }

    // ================== 下面是三个独立的具体业务逻辑 ==================

    /**
     * 服务 A：调用岗位匹配度计算
     * 这里写具体的 HTTP 请求逻辑，比如调 Python 的 /api/match
     */
    private String callMatchDegreeService(LoginUser user) {
        log.info(">>> [服务A-匹配度] 开始执行... 线程: {}", Thread.currentThread().getName());
        try {
            // TODO: 这里替换成你真实的 HTTP 调用代码
            // String json = HttpUtils.post("http://ai-service/match", params);
            // return parse(json);

            Thread.sleep(2000); // 模拟耗时 2秒
            return "95% (高匹配)";
        } catch (Exception e) {
            log.error("匹配度计算服务异常", e);
            return "计算服务不可用";
        }
    }

    /**
     * 服务 B：调用面试表现打分
     * 这里写具体的 HTTP 请求逻辑，比如调 Python 的 /api/score
     */
    private String callInterviewScoringService(LoginUser user) {
        log.info(">>> [服务B-面试打分] 开始执行... 线程: {}", Thread.currentThread().getName());
        try {
            // TODO: 这里替换成你真实的 HTTP 调用代码

            Thread.sleep(3000); // 模拟耗时 3秒
            return "88分 (表现良好)";
        } catch (Exception e) {
            log.error("面试打分服务异常", e);
            return "打分服务不可用";
        }
    }

    /**
     * 服务 C：调用优缺点分析
     * 这里写具体的 HTTP 请求逻辑，比如调 Python 的 /api/analysis
     */
    private String callProsConsAnalysisService(LoginUser user) {
        log.info(">>> [服务C-优缺点] 开始执行... 线程: {}", Thread.currentThread().getName());
        try {
            // TODO: 这里替换成你真实的 HTTP 调用代码

            Thread.sleep(1500); // 模拟耗时 1.5秒
            return "优点：逻辑清晰；缺点：语速稍快";
        } catch (Exception e) {
            log.error("优缺点分析服务异常", e);
            return "分析服务不可用";
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