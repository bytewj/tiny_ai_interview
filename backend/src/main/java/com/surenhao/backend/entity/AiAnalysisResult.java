package com.surenhao.backend.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiAnalysisResult {
    private String jobMatchAnalysis;      // 岗位匹配度结果
    private String interviewScore;        // 面试表现打分
    private String prosAndCons;           // 优缺点分析
    private String totalTimeCost;         // 总耗时（用于展示优化效果）
}