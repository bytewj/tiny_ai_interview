package com.surenhao.backend.service.impl;

import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.service.AiService;
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
    @Qualifier("aiTaskExecutor") // 注入在 AiThreadPoolConfig 中定义的线程池
    private ExecutorService executor;

    @Override
    public AiAnalysisResult analyzeParallel() {
        long start = System.currentTimeMillis();
        log.info("=== 开始并行执行 AI 分析任务 ===");

        // 1. 开启异步任务 A：岗位匹配 (20s)
        CompletableFuture<String> taskA = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("岗位匹配度计算", 20);
        }, executor);

        // 2. 开启异步任务 B：面试打分 (12s)
        CompletableFuture<String> taskB = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("面试表现打分", 12);
        }, executor);

        // 3. 开启异步任务 C：优缺点分析 (21s)
        CompletableFuture<String> taskC = CompletableFuture.supplyAsync(() -> {
            return mockAiInference("优缺点分析", 21);
        }, executor);

        // 4. 等待所有任务完成 (阻塞主线程直到最慢的一个完成)
        // join() 会阻塞直到所有任务都 done
        CompletableFuture.allOf(taskA, taskB, taskC).join();

        // 5. 获取结果并聚合
        try {
            // 因为上面已经 join 了，这里 get 是立即返回的
            String resA = taskA.get();
            String resB = taskB.get();
            String resC = taskC.get();

            long totalTime = System.currentTimeMillis() - start;
            log.info("=== 并行任务结束，总耗时: {} ms ===", totalTime);

            return new AiAnalysisResult(resA, resB, resC, totalTime + " ms");

        } catch (Exception e) {
            log.error("AI 任务聚合失败", e);
            // 兜底返回，防止前端报错
            return new AiAnalysisResult("服务繁忙", "服务繁忙", "服务繁忙", "异常");
        }
    }

    /**
     * 模拟耗时的 AI 推理过程 (私有辅助方法)
     */
    private String mockAiInference(String taskName, int seconds) {
        long start = System.currentTimeMillis();
        log.info(">>> [{}] 开始执行 (线程: {})", taskName, Thread.currentThread().getName());
        try {
            // 模拟高延迟 IO
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态，良好的编程习惯
            e.printStackTrace();
        }
        log.info("<<< [{}] 执行完成，耗时 {}s", taskName, seconds);
        return String.format("[%s] 完成 | 耗时%ds", taskName, seconds);
    }
}