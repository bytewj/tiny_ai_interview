package com.surenhao.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 用于处理 AI 推理 IO 密集型任务的线程池配置
 */
@Configuration
public class AiThreadPoolConfig {

    @Bean("aiTaskExecutor") // 给 Bean 起名字，方便在 Service 中指定注入
    public ExecutorService aiTaskExecutor() {
        // 任务队列未到达队列容量时，最大可以同时运行的线程数量
        int corePoolSize = 10;
        // 任务队列中存放的任务到达队列容量，当前可以同时运行的线程数量变为最大线程数
        int maximumPoolSize = 20;
        // 线程池中的线程数量大于 corePoolSize 的时候，如果这时没有新任务提交，
        // 核心线程外的线程不会立即销毁，而是会等待，直到等待的时间超过了 keepAliveTime 才会被回收销毁。
        long keepAliveTime = 60;
        // 任务队列，使用有界队列防止 OOM
        // 新任务来的时候会先判断当前运行的线程数量是否达到核心线程数，如果达到的话就会被存放在队列中
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(100);

        // 自定义线程工厂，给线程起个名字，方便出问题时排查
        // executor 创建新线程的时候会用到
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger count = new AtomicInteger(1);
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("AI-Thread-" + count.getAndIncrement());
                return t;
            }
        };

        // 拒绝策略：如果队列满了，由调用者线程自己执行（CallerRunsPolicy），保证不丢任务
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                workQueue,
                threadFactory,
                handler
        );
    }
}