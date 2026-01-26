package com.surenhao.backend.controller;

import com.surenhao.backend.annotation.Public;
import com.surenhao.backend.common.Result;
import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.entity.AiChatRequest;
import com.surenhao.backend.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult; // 必须引入这个
import reactor.core.publisher.Flux;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * 获取 AI 面试分析报告 (异步非阻塞版)
     * * 修改点：
     * 1. 返回值改为 DeferredResult<Result<AiAnalysisResult>>
     * 2. 方法内部不再阻塞等待，而是注册回调
     */
    @GetMapping("/report")
    public DeferredResult<Result<AiAnalysisResult>> getAiReport() {
        // 1. 创建 DeferredResult，设置超时时间 (例如 10000ms = 10秒)
        // 这里的泛型要包两层：外层是 Spring 异步机制，内层是你自定义的统一返回包装
        DeferredResult<Result<AiAnalysisResult>> output = new DeferredResult<>(10000L);

        // 2. 定义超时逻辑：如果 10秒 AI 还没算完，给前端返回超时提示
        output.onTimeout(() -> {
            output.setErrorResult(Result.fail("AI 分析超时，请稍后重试"));
        });

        // 3. 定义错误回调：如果发生其他未知异常
        output.onError((Throwable t) -> {
            output.setErrorResult(Result.fail("系统内部错误：" + t.getMessage()));
        });

        // 4. 调用 Service (注意：Service 必须返回 CompletableFuture，不能是 void 或实体)
        // 这一步是瞬时的，Tomcat 线程不会在这里卡住
        CompletableFuture<AiAnalysisResult> future = aiService.analyzeParallel();

        // 5. 注册回调：等后台 AI 算完了，自动执行这里
        future.thenAccept(analysisResult -> {
            // 成功拿到结果，通过 output.setResult 唤醒 Spring 返回数据
            output.setResult(Result.data(analysisResult));
        }).exceptionally(ex -> {
            // 如果 Service 里报错了 (比如 AI 服务挂了)
            output.setErrorResult(Result.fail("AI 服务调用失败: " + ex.getMessage()));
            return null;
        });

        // 6. Tomcat 线程立刻释放，去处理其他请求
        return output;
    }

    /**
     * SSE 流式对话接口 (保持不变)
     */
    @Public
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> chat(@RequestBody @Validated AiChatRequest request) {
        return aiService.streamChat(request.getMessage());
    }
}