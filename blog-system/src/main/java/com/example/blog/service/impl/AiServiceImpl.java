package com.example.blog.service.impl;

import com.example.blog.config.AiProperties;
import com.example.blog.dto.AiChatMessage;
import com.example.blog.dto.AiChatRequest;
import com.example.blog.service.AiService;
import com.example.blog.service.support.RagKnowledgeService;
import com.example.blog.service.support.dto.RagSearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * AI 对话服务实现，负责配额校验、RAG 检索增强和模型调用。
 */
@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);
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

    @Resource
    private RagKnowledgeService ragKnowledgeService;

    /**
     * 获取指定用户当前的 AI 调用额度信息。
     *
     * @param userId 当前用户 ID
     * @return 额度信息
     */
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

    /**
     * 处理一次 AI 对话请求，并在可用时追加 RAG 检索上下文。
     *
     * @param userId 当前用户 ID
     * @param request 对话请求参数
     * @return 包含回答、模型和来源信息的结果
     */
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
        RagSearchResult ragResult = ragKnowledgeService.search(message.trim());
        String answer = callProvider(message.trim(), request == null ? null : request.getHistory(), resolvedModel, ragResult);
        int currentUsed = incrementUsage(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("provider", aiProperties.getProvider());
        result.put("model", resolvedModel);
        result.put("mode", ragResult.isHasRelevantContext() ? "rag" : "chat");
        result.put("sources", ragResult.getSources());
        result.put("dailyLimit", dailyLimit);
        result.put("used", currentUsed);
        result.put("remaining", Math.max(0, dailyLimit - currentUsed));
        return result;
    }

    /**
     * 校验 AI 功能是否已开启。
     */
    private void ensureEnabled() {
        if (!aiProperties.isEnabled()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "AI 功能暂未开启");
        }
    }

    /**
     * 根据配置分发请求到对应的大模型提供方。
     *
     * @param message 用户问题
     * @param history 最近的对话历史
     * @param model 最终解析出的模型名称
     * @param ragResult 检索结果
     * @return 模型回答
     */
    private String callProvider(String message, List<AiChatMessage> history, String model, RagSearchResult ragResult) {
        String provider = aiProperties.getProvider() == null ? "openai" : aiProperties.getProvider().trim().toLowerCase();
        if ("ollama".equals(provider)) {
            return callOllama(message, history, model, ragResult);
        }
        return callOpenAiCompatible(message, history, model, ragResult);
    }

    /**
     * 调用兼容 OpenAI 的聊天补全接口。
     *
     * @param message 用户问题
     * @param history 最近的对话历史
     * @param model 最终解析出的模型名称
     * @param ragResult 检索结果
     * @return 模型回答文本
     */
    private String callOpenAiCompatible(String message, List<AiChatMessage> history, String model, RagSearchResult ragResult) {
        List<Map<String, String>> messages = buildMessages(message, history, ragResult);

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

    /**
     * 调用 Ollama 聊天接口。
     *
     * @param message 用户问题
     * @param history 最近的对话历史
     * @param model 最终解析出的模型名称
     * @param ragResult 检索结果
     * @return 模型回答文本
     */
    private String callOllama(String message, List<AiChatMessage> history, String model, RagSearchResult ragResult) {
        List<Map<String, String>> messages = buildMessages(message, history, ragResult);

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

    /**
     * 构建最终发送给大模型的消息列表。
     *
     * @param message 用户问题
     * @param history 最近的对话历史
     * @param ragResult 检索结果
     * @return 面向不同提供方的消息列表
     */
    private List<Map<String, String>> buildMessages(String message, List<AiChatMessage> history, RagSearchResult ragResult) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(createMessage("system", aiProperties.getSystemPrompt()));
        messages.add(createMessage("system", buildRagInstruction(ragResult)));

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

    /**
     * 为当前请求生成 RAG 专用系统提示词。
     *
     * @param ragResult 检索结果
     * @return 系统提示词文本
     */
    private String buildRagInstruction(RagSearchResult ragResult) {
        if (ragResult != null && ragResult.isHasRelevantContext() && !ragResult.getSources().isEmpty()) {
            return "你现在是网站的 RAG 助手。请优先根据下面检索到的站内文章和新闻内容回答，"
                    + "不要把未出现在上下文里的站内信息说成已确认事实。"
                    + "如果上下文不足，请明确说明站内知识库信息不足，再给出谨慎建议。\n\n"
                    + "检索上下文：\n"
                    + ragResult.getContextBlock();
        }
        return "当前没有检索到足够相关的站内文章或新闻。请先明确说明站内知识库暂未命中，"
                + "再给出简洁、保守的通用建议。";
    }

    /**
     * 创建单条提供方消息结构。
     *
     * @param role 对话角色
     * @param content 消息内容
     * @return 提供方消息映射
     */
    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    /**
     * 创建通用 JSON 请求头。
     *
     * @return JSON 请求头
     */
    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));
        return headers;
    }

    /**
     * 执行 AI 请求并解析返回的 JSON 响应。
     *
     * @param url 提供方接口地址
     * @param body 请求体
     * @param headers 请求头
     * @return 解析后的 JSON 数据
     */
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

    /**
     * 从 Redis 读取当前用户当天的已用次数。
     * Redis 不可用时降级为 0，避免对话功能整体不可用。
     *
     * @param userId 当前用户 ID
     * @return 已用次数
     */
    private int getUsedCount(Long userId) {
        try {
            String value = stringRedisTemplate.opsForValue().get(buildQuotaKey(userId));
            if (value == null || value.isBlank()) {
                return 0;
            }
            return Integer.parseInt(value);
        } catch (NumberFormatException error) {
            return 0;
        } catch (Exception error) {
            log.warn("读取 AI 配额失败，已按 0 次降级处理，userId={}", userId, error);
            return 0;
        }
    }

    /**
     * 增加当前用户当天的使用次数。
     * Redis 不可用时降级返回 1，保证主流程仍可继续。
     *
     * @param userId 当前用户 ID
     * @return 增加后的次数
     */
    private int incrementUsage(Long userId) {
        try {
            String key = buildQuotaKey(userId);
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, ttlToTomorrow());
            }
            return count == null ? 0 : count.intValue();
        } catch (Exception error) {
            log.warn("写入 AI 配额失败，已按临时次数返回，userId={}", userId, error);
            return 1;
        }
    }

    /**
     * 计算距离下一天零点的剩余时长。
     *
     * @return 到明天的剩余时长
     */
    private Duration ttlToTomorrow() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, tomorrow);
    }

    /**
     * 构造指定用户当天的 Redis 限额键。
     *
     * @param userId 当前用户 ID
     * @return Redis 键
     */
    private String buildQuotaKey(Long userId) {
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return "ai:quota:" + today + ":" + userId;
    }

    /**
     * 去掉提供方基础地址末尾的斜杠。
     *
     * @param baseUrl 配置中的基础地址
     * @return 归一化后的基础地址
     */
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

    /**
     * 根据请求参数和配置解析最终使用的模型名称。
     *
     * @param requestedModel 客户端请求的模型名称
     * @return 最终模型名称
     */
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

    /**
     * 安全地将对象转换为 Map。
     *
     * @param value 源对象
     * @return 转换后的 Map，不可转换时返回空 Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        if (value instanceof Map) {
            return (Map<String, Object>) value;
        }
        return new HashMap<>();
    }

    /**
     * 安全地将对象转换为 Map 列表。
     *
     * @param value 源对象
     * @return 转换后的列表，不可转换时返回空列表
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }
}
