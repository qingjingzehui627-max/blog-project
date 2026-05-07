package com.example.blog.dto;

import lombok.Data;

/**
 * 表示一次 RAG 检索返回的单条来源信息。
 */
@Data
public class AiSourceItem {
    private String type;
    private Long documentId;
    private String title;
    private String url;
    private String snippet;
}
