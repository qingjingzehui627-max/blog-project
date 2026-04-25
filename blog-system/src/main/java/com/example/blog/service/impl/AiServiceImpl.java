package com.example.blog.service.impl;

import com.example.blog.config.AiProperties;
import com.example.blog.dto.AiChatMessage;
import com.example.blog.dto.AiChatRequest;
import com.example.blog.service.AiService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AiServiceImpl implements AiService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {
    };

    @Resource
    private AiProperties aiProperties;

    @Resource(name = "aiRestTemplate")
    private RestTemplate restTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public Map<String, Object> getQuota(Long userId) {
        int used = getUsedCount(userId);
        int dailyLimit = aiProperties.getDailyLimit();
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", aiProperties.isEnabled());
        result.put("provider", aiProperties.getProvider());
        result.put("model", aiProperties.getModel());
        result.put("dailyLimit", dailyLimit);
        result.put("used", used);
        result.put("remaining", Math.max(0, dailyLimit - used));
        return result;
    }

    @Override
    public Map<String, Object> chat(Long userId, AiChatRequest request) {
        ensureEnabled();
        String message = request == null ? null : request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "问题不能为空");
        }

        int dailyLimit = aiProperties.getDailyLimit();
        int used = getUsedCount(userId);
        if (used >= dailyLimit) {
            throw new ResponseStatusException(FORBIDDEN, "今日提问次数已用完");
        }

        String resolvedModel = resolveModel(request == null ? null : request.getModel());
        String answer = callProvider(message.trim(), request == null ? null : request.getHistory(), resolvedModel);
        int currentUsed = incrementUsage(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("provider", aiProperties.getProvider());
        result.put("model", resolvedModel);
        result.put("dailyLimit", dailyLimit);
        result.put("used", currentUsed);
        result.put("remaining", Math.max(0, dailyLimit - currentUsed));
        return result;
    }

    private void ensureEnabled() {
        if (!aiProperties.isEnabled()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 功能暂未开启");
        }
    }

    private String callProvider(String message, List<AiChatMessage> history, String model) {
        String provider = aiProperties.getProvider() == null ? "openai" : aiProperties.getProvider().trim().toLowerCase();
        if ("ollama".equals(provider)) {
            return callOllama(message, history, model);
        }
        return callOpenAiCompatible(message, history, model);
    }

    private String callOpenAiCompatible(String message, List<AiChatMessage> history, String model) {
        List<Map<String, String>> messages = buildMessages(message, history);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", aiProperties.getTemperature());

        HttpHeaders headers = createJsonHeaders();
        if (aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank()) {
            headers.setBearerAuth(aiProperties.getApiKey().trim());
        }

        String url = trimTrailingSlash(aiProperties.getBaseUrl()) + "/chat/completions";
        Map<String, Object> response = readJsonResponse(url, body, headers);

        List<Map<String, Object>> choices = castList(response.get("choices"));
        if (choices.isEmpty()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 返回内容为空");
        }
        Map<String, Object> choice = castMap(choices.get(0));
        Map<String, Object> replyMessage = castMap(choice.get("message"));
        Object content = replyMessage.get("content");
        if (content instanceof String) {
            return ((String) content).trim();
        }
        if (content instanceof List) {
            StringBuilder builder = new StringBuilder();
            for (Object item : (List<?>) content) {
                Map<String, Object> part = castMap(item);
                Object text = part.get("text");
                if (text != null) {
                    builder.append(text);
                }
            }
            return builder.toString().trim();
        }
        throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 返回内容格式不支持");
    }

    private String callOllama(String message, List<AiChatMessage> history, String model) {
        List<Map<String, String>> messages = buildMessages(message, history);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", false);

        String url = trimTrailingSlash(aiProperties.getBaseUrl()) + "/api/chat";
        Map<String, Object> response = readJsonResponse(url, body, createJsonHeaders());
        Map<String, Object> replyMessage = castMap(response.get("message"));
        Object content = replyMessage.get("content");
        if (content == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 返回内容为空");
        }
        return String.valueOf(content).trim();
    }

    private List<Map<String, String>> buildMessages(String message, List<AiChatMessage> history) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(createMessage("system", aiProperties.getSystemPrompt()));

        if (history != null && !history.isEmpty()) {
            int maxHistory = Math.max(0, aiProperties.getMaxHistory());
            int start = Math.max(0, history.size() - maxHistory);
            for (AiChatMessage item : history.subList(start, history.size())) {
                if (item == null || item.getRole() == null || item.getContent() == null) {
                    continue;
                }
                String role = item.getRole().trim().toLowerCase();
                if (!"user".equals(role) && !"assistant".equals(role)) {
                    continue;
                }
                String content = item.getContent().trim();
                if (content.isEmpty()) {
                    continue;
                }
                messages.add(createMessage(role, content));
            }
        }

        messages.add(createMessage("user", message));
        return messages;
    }

    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    private Map<String, Object> readJsonResponse(String url, Map<String, Object> body, HttpHeaders headers) {
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        String responseBody = responseEntity.getBody();
        MediaType contentType = responseEntity.getHeaders().getContentType();

        if (responseBody == null || responseBody.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 接口返回为空");
        }

        String trimmed = responseBody.trim();
        boolean looksLikeJson = trimmed.startsWith("{") || trimmed.startsWith("[");
        boolean isJsonContentType = contentType != null && (
                MediaType.APPLICATION_JSON.includes(contentType) ||
                        contentType.getSubtype().toLowerCase().contains("json")
        );

        if (!looksLikeJson && !isJsonContentType) {
            throw new ResponseStatusException(
                    SERVICE_UNAVAILABLE,
                    "AI 接口返回了 HTML 页面，请检查 ai.base-url、provider 或 api-key 配置"
            );
        }

        try {
            return objectMapper.readValue(trimmed, MAP_TYPE);
        } catch (Exception error) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 接口返回的不是有效 JSON");
        }
    }

    private int getUsedCount(Long userId) {
        String value = stringRedisTemplate.opsForValue().get(buildQuotaKey(userId));
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return 0;
        }
    }

    private int incrementUsage(Long userId) {
        String key = buildQuotaKey(userId);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttlToTomorrow());
        }
        return count == null ? 0 : count.intValue();
    }

    private Duration ttlToTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, tomorrow);
    }

    private String buildQuotaKey(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return "ai:quota:" + today + ":" + userId;
    }

    private String trimTrailingSlash(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "";
        }
        String url = baseUrl.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String resolveModel(String requestedModel) {
        List<String> availableModels = aiProperties.getModels();
        if (availableModels == null || availableModels.isEmpty()) {
            return requestedModel != null && !requestedModel.isBlank() ? requestedModel.trim() : aiProperties.getModel();
        }
        if (requestedModel != null) {
            String normalized = requestedModel.trim();
            for (String item : availableModels) {
                if (item != null && item.trim().equalsIgnoreCase(normalized)) {
                    return item.trim();
                }
            }
        }
        return aiProperties.getModel();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }
}
