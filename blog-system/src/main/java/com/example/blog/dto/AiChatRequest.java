package com.example.blog.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {
    private String message;
    private String model;
    private List<AiChatMessage> history;
}
