package com.example.blog.service.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 新闻内容规范化工具。
 */
@Component
public class NewsContentNormalizer {

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[^a-z0-9\\s]");
    private static final List<String> TITLE_PREFIXES = Arrays.asList(
            "breaking:",
            "analysis:",
            "live:",
            "exclusive:"
    );
    private static final List<String> DROP_QUERY_PREFIXES = Arrays.asList("utm_", "spm");
    private static final List<String> DROP_QUERY_KEYS = Arrays.asList("fbclid", "gclid", "igshid", "mc_cid", "mc_eid", "ref");

    /**
     * 规范化新闻链接。
     *
     * @param url 原始链接
     * @return 规范化后的链接
     */
    public String normalizeUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "";
        }

        try {
            URI uri = new URI(url.trim());
            String host = uri.getHost();
            if (!StringUtils.hasText(host)) {
                return url.trim().toLowerCase(Locale.ROOT);
            }

            String path = StringUtils.hasText(uri.getPath()) ? uri.getPath() : "";
            if (path.endsWith("/") && path.length() > 1) {
                path = path.substring(0, path.length() - 1);
            }

            Map<String, String> queryMap = parseQuery(uri.getRawQuery());
            StringBuilder builder = new StringBuilder();
            builder.append(host.toLowerCase(Locale.ROOT)).append(path.toLowerCase(Locale.ROOT));
            if (!queryMap.isEmpty()) {
                builder.append("?");
                boolean first = true;
                for (Map.Entry<String, String> entry : queryMap.entrySet()) {
                    if (!first) {
                        builder.append("&");
                    }
                    builder.append(entry.getKey()).append("=").append(entry.getValue());
                    first = false;
                }
            }
            return builder.toString();
        } catch (URISyntaxException exception) {
            return url.trim().toLowerCase(Locale.ROOT);
        }
    }

    /**
     * 规范化新闻标题。
     *
     * @param title 原始标题
     * @return 规范化后的标题
     */
    public String normalizeTitle(String title) {
        if (!StringUtils.hasText(title)) {
            return "";
        }

        String normalized = title.trim().toLowerCase(Locale.ROOT);
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFKC);
        for (String prefix : TITLE_PREFIXES) {
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length()).trim();
                break;
            }
        }

        // 常见媒体标题会在末尾拼接站点名，这里优先保留真正的新闻标题主体。
        normalized = stripTrailingPublisher(normalized, " - ");
        normalized = stripTrailingPublisher(normalized, " | ");
        normalized = PUNCTUATION_PATTERN.matcher(normalized).replaceAll(" ");
        normalized = MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
        return normalized;
    }

    /**
     * 计算字符串哈希值。
     *
     * @param value 原始字符串
     * @return SHA-256 哈希值
     */
    public String hash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 算法不可用", exception);
        }
    }

    /**
     * 计算标题相似度。
     *
     * @param leftTitle 标题一
     * @param rightTitle 标题二
     * @return 相似度分值
     */
    public double calculateTitleSimilarity(String leftTitle, String rightTitle) {
        String left = normalizeTitle(leftTitle);
        String right = normalizeTitle(rightTitle);
        if (!StringUtils.hasText(left) || !StringUtils.hasText(right)) {
            return 0D;
        }
        if (left.equals(right)) {
            return 1D;
        }

        List<String> leftTokens = tokenize(left);
        List<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return 0D;
        }

        long intersection = leftTokens.stream().filter(rightTokens::contains).distinct().count();
        long union = leftTokens.stream().distinct().count()
                + rightTokens.stream().filter(token -> !leftTokens.contains(token)).count();
        return union == 0 ? 0D : (double) intersection / union;
    }

    /**
     * 解析并过滤查询参数。
     *
     * @param rawQuery 原始查询参数
     * @return 过滤后的查询参数
     */
    private Map<String, String> parseQuery(String rawQuery) {
        Map<String, String> queryMap = new LinkedHashMap<>();
        if (!StringUtils.hasText(rawQuery)) {
            return queryMap;
        }

        String[] pairs = rawQuery.split("&");
        List<String[]> parsedPairs = new ArrayList<>();
        for (String pair : pairs) {
            if (!StringUtils.hasText(pair)) {
                continue;
            }
            String[] split = pair.split("=", 2);
            String key = split[0].toLowerCase(Locale.ROOT);
            if (shouldDropQueryKey(key)) {
                continue;
            }
            String value = split.length > 1 ? split[1] : "";
            parsedPairs.add(new String[]{key, value});
        }

        parsedPairs.stream()
                .sorted(Comparator.comparing(item -> item[0]))
                .forEach(item -> queryMap.put(item[0], item[1]));
        return queryMap;
    }

    /**
     * 判断是否需要移除当前查询参数。
     *
     * @param key 参数名
     * @return true 表示需要移除
     */
    private boolean shouldDropQueryKey(String key) {
        if (DROP_QUERY_KEYS.contains(key)) {
            return true;
        }
        return DROP_QUERY_PREFIXES.stream().anyMatch(key::startsWith);
    }

    /**
     * 去除标题尾部的媒体名后缀。
     *
     * @param title 原始标题
     * @param separator 分隔符
     * @return 去除后的标题
     */
    private String stripTrailingPublisher(String title, String separator) {
        int index = title.lastIndexOf(separator);
        if (index <= 0) {
            return title;
        }
        String tail = title.substring(index + separator.length()).trim();
        return tail.length() <= 24 ? title.substring(0, index).trim() : title;
    }

    /**
     * 将标题拆分为词元列表。
     *
     * @param normalizedTitle 已规范化标题
     * @return 词元列表
     */
    private List<String> tokenize(String normalizedTitle) {
        return Arrays.stream(normalizedTitle.split(" "))
                .filter(StringUtils::hasText)
                .toList();
    }
}
