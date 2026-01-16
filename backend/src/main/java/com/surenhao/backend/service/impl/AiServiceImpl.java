package com.surenhao.backend.service.impl;

import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.entity.SysUser;
import com.surenhao.backend.service.AiService;
import com.surenhao.backend.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class AiServiceImpl implements AiService {

    @Autowired
    @Qualifier("aiTaskExecutor")
    private ExecutorService executor;

    @Override
    public AiAnalysisResult analyzeParallel() {
        long start = System.currentTimeMillis();
        // 此时在主线程，能拿到用户
        SysUser currentUser = UserContext.get();
        log.info("=== 开始并行执行 AI 分析任务 (用户: {}) ===", currentUser.getNickname());

        // 1. 开启异步任务 A
        // 🔥 注意：这里不需要再手动传 UserContext 了！TTL 会自动搞定！
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("岗位匹配度计算", 20);
        }, executor);

        // 2. 开启异步任务 B
        CompletableFuture<String> taskB = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("面试表现打分", 12);
        }, executor);

        // 3. 开启异步任务 C
        CompletableFuture<String> taskC = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("优缺点分析", 21);
        }, executor);

        // 等待所有任务完成
        CompletableFuture.allOf(taskA, taskB, taskC).join();

        try {
            String resA = taskA.get();
            String resB = taskB.get();
            String resC = taskC.get();

            long totalTime = System.currentTimeMillis() - start;
            log.info("=== 并行任务结束，总耗时: {} ms ===", totalTime);
            return new AiAnalysisResult(resA, resB, resC, totalTime + " ms");

        } catch (Exception e) {
            log.error("AI 任务聚合失败", e);
            return new AiAnalysisResult("失败", "失败", "失败", "异常");
        }
    }

    private String mockAiInference(String taskName, int seconds) {
        // 🔥 验证点：这里在子线程里，直接 get() 就能拿到主线程的用户
        SysUser user = UserContext.get();

        // 如果 TTL 没生效，这里 user 会是 null，报空指针异常
        log.info(">>> [{}] 开始执行 (用户: {}, 线程: {})", taskName, user.getNickname(), Thread.currentThread().getName());

        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("<<< [{}] 执行完成", taskName);
        return String.format("[%s] 完成 | 耗时%ds", taskName, seconds);
    }
}