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

/**
 * AI 能力相关接口控制器。
 */
@RestController
@RequestMapping(value = "/api/ai", produces = "application/json;charset=UTF-8")
public class AiController {

    @Resource
    private AiProperties aiProperties;

    @Resource
    private AiService aiService;

    /**
     * 获取 AI 配置摘要。
     *
     * @return 前端展示所需的 AI 配置
     */
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
        result.put("ragEnabled", aiProperties.isRagEnabled());
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前登录用户的 AI 配额信息。
     *
     * @param principal 当前登录用户
     * @return 配额信息
     */
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

    /**
     * 提交 AI 对话请求。
     *
     * @param request 对话参数
     * @param principal 当前登录用户
     * @return AI 回答结果
     */
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

    /**
     * 统一封装错误响应。
     *
     * @param message 错误消息
     * @return 错误响应体
     */
    private Map<String, Object> error(String message) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", message);
        return result;
    }
}
