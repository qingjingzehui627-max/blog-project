package com.example.blog.service;

import com.example.blog.dto.AiChatRequest;

import java.util.Map;

public interface AiService {
    Map<String, Object> getQuota(Long userId);

    Map<String, Object> chat(Long userId, AiChatRequest request);
}
