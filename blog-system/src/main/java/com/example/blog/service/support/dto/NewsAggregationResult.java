package com.example.blog.service.support.dto;

import lombok.Data;

/**
 * 单个数据源的聚合写入结果。
 */
@Data
public class NewsAggregationResult {
    private int fetchedCount;
    private int insertedCount;
    private int updatedCount;
    private int skippedCount;
}
