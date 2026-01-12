package com.surenhao.backend.controller;

import com.surenhao.backend.common.Result; // 引入自定义返回结果
import com.surenhao.backend.entity.AiAnalysisResult;
import com.surenhao.backend.service.AiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /**
     * 获取 AI 面试分析报告
     * * 默认安全策略：
     * 这里没有加 @Public 注解，所以 AOP 会自动拦截检查登录状态。
     * 如果 ThreadLocal 里没用户，直接抛 401。
     */
    @GetMapping("/report")
    public Result<AiAnalysisResult> getAiReport() {
        // 调用并行编排的服务
        AiAnalysisResult result = aiService.analyzeParallel();
        return Result.data(result);
    }
}