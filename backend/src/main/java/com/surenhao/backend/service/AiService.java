package com.surenhao.backend.service;

import com.surenhao.backend.entity.AiAnalysisResult;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

/**
 * AI 业务接口层
 */
public interface AiService {

    /**
     * 并行执行 AI 面试分析任务 (多线程处理)
     * @return 分析结果聚合对象
     */
    AiAnalysisResult analyzeParallel();

    /**
     * SSE 流式对话并持久化 (响应式处理)
     * @param message 用户提问内容
     * @return SSE 数据流
     */
    Flux<ServerSentEvent<String>> streamChat(@NotBlank(message = "提问内容不能为空") String message);
}