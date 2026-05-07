package com.example.blog.service.support.dto;

import com.example.blog.dto.AiSourceItem;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 封装用于增强 AI 回答的检索结果。
 */
@Data
public class RagSearchResult {
    private boolean hasRelevantContext;
    private String contextBlock = "";
    private List<AiSourceItem> sources = new ArrayList<>();
}
