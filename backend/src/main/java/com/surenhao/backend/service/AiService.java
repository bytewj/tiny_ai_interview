package com.surenhao.backend.service;

import com.surenhao.backend.entity.AiAnalysisResult;

/**
 * AI 业务接口
 */
public interface AiService {
    /**
     * 并行执行 AI 分析任务
     * @return 分析结果聚合对象
     */
    AiAnalysisResult analyzeParallel();
}