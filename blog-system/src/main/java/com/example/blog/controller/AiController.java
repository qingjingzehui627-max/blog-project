package com.example.blog.controller;

import com.example.blog.config.AiProperties;
import com.example.blog.dto.AiChatRequest;
import com.example.blog.service.AiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiProperties aiProperties;

    @Resource
    private AiService aiService;

    @GetMapping("/config")
    public ResponseEntity<?> getConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", aiProperties.isEnabled());
        result.put("provider", aiProperties.getProvider());
        result.put("model", aiProperties.getModel());
        result.put("models", aiProperties.getModels() == null || aiProperties.getModels().isEmpty()
                ? java.util.Collections.singletonList(aiProperties.getModel())
                : aiProperties.getModels());
        result.put("dailyLimit", aiProperties.getDailyLimit());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/quota")
    public ResponseEntity<?> getQuota(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(error("请先登录后再使用 AI"));
        }
        Long userId = Long.parseLong(principal.getName());
        try {
            return ResponseEntity.ok(aiService.getQuota(userId));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatus()).body(error(exception.getReason()));
        }
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody AiChatRequest request, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(error("请先登录后再使用 AI"));
        }
        Long userId = Long.parseLong(principal.getName());
        try {
            return ResponseEntity.ok(aiService.chat(userId, request));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatus()).body(error(exception.getReason()));
        }
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        return result;
    }
}
