package com.example.blog.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.config.AiProperties;
import com.example.blog.dto.AiSourceItem;
import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.Post;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.mapper.PostMapper;
import com.example.blog.service.support.dto.RagSearchResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAG 知识检索服务，负责从文章和新闻中构建回答上下文。
 */
@Service
public class RagKnowledgeService {

    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern LATIN_TOKEN_PATTERN = Pattern.compile("[a-z0-9]{2,}");
    private static final Pattern CJK_SEGMENT_PATTERN = Pattern.compile("[\\u4e00-\\u9fff]{2,}");
    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    @Resource
    private PostMapper postMapper;

    @Resource
    private NewsArticleMapper newsArticleMapper;

    @Resource
    private AiProperties aiProperties;

    /**
     * 检索博客文章和新闻内容，并返回最相关的上下文片段。
     *
     * @param query 用户问题文本
     * @return 包含上下文和来源信息的检索结果
     */
    public RagSearchResult search(String query) {
        RagSearchResult result = new RagSearchResult();
        if (!StringUtils.hasText(query) || !aiProperties.isRagEnabled()) {
            return result;
        }

        List<String> tokens = tokenizeQuery(query);
        List<ScoredChunk> rankedChunks = new ArrayList<>();
        rankedChunks.addAll(scorePostChunks(query, tokens));
        rankedChunks.addAll(scoreNewsChunks(query, tokens));
        rankedChunks.sort(Comparator.comparingDouble(ScoredChunk::getScore).reversed());

        Map<String, ScoredChunk> bestByDocument = new LinkedHashMap<>();
        for (ScoredChunk chunk : rankedChunks) {
            if (chunk.getScore() <= 0D) {
                continue;
            }
            bestByDocument.putIfAbsent(chunk.getDocumentKey(), chunk);
            if (bestByDocument.size() >= aiProperties.getRagMaxSources()) {
                break;
            }
        }

        if (bestByDocument.isEmpty()) {
            return result;
        }

        List<AiSourceItem> sources = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();
        int index = 1;
        for (ScoredChunk chunk : bestByDocument.values()) {
            AiSourceItem source = new AiSourceItem();
            source.setType(chunk.getTypeLabel());
            source.setDocumentId(chunk.getDocumentId());
            source.setTitle(chunk.getTitle());
            source.setUrl(chunk.getUrl());
            source.setSnippet(chunk.getSnippet());
            sources.add(source);

            contextBuilder.append("[").append(index).append("] ")
                    .append(chunk.getTypeLabel()).append("\n")
                    .append("标题：").append(chunk.getTitle()).append("\n");
            if (StringUtils.hasText(chunk.getUrl())) {
                contextBuilder.append("链接：").append(chunk.getUrl()).append("\n");
            }
            contextBuilder.append("内容片段：").append(chunk.getSnippet()).append("\n\n");
            index++;
        }

        result.setHasRelevantContext(true);
        result.setSources(sources);
        result.setContextBlock(contextBuilder.toString().trim());
        return result;
    }

    /**
     * 对博客文章切分后的内容块进行打分。
     *
     * @param query 用户原始问题
     * @param tokens 归一化后的检索词
     * @return 按相关性筛出的文章候选块
     */
    private List<ScoredChunk> scorePostChunks(String query, List<String> tokens) {
        int limit = Math.max(10, aiProperties.getRagCandidateLimit());
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>()
                        .orderByDesc(Post::getCreatedAt)
                        .last("LIMIT " + limit)
        );

        List<ScoredChunk> results = new ArrayList<>();
        for (Post post : posts) {
            String cleanTitle = cleanText(post.getTitle());
            String cleanContent = cleanText(post.getContent());
            if (!StringUtils.hasText(cleanTitle) && !StringUtils.hasText(cleanContent)) {
                continue;
            }
            for (String chunkText : splitIntoChunks(cleanTitle + "\n" + cleanContent)) {
                double score = scoreChunk(query, tokens, cleanTitle, chunkText, post.getCreatedAt(), false);
                if (score <= 0D) {
                    continue;
                }
                results.add(new ScoredChunk(
                        "post:" + post.getId(),
                        post.getId(),
                        "文章",
                        cleanTitle,
                        "/post/" + post.getId(),
                        buildSnippet(chunkText, tokens),
                        score
                ));
            }
        }
        return results;
    }

    /**
     * 对新闻切分后的内容块进行打分。
     *
     * @param query 用户原始问题
     * @param tokens 归一化后的检索词
     * @return 按相关性筛出的新闻候选块
     */
    private List<ScoredChunk> scoreNewsChunks(String query, List<String> tokens) {
        int limit = Math.max(10, aiProperties.getRagCandidateLimit());
        List<NewsArticle> articles = newsArticleMapper.selectList(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getStatus, 1)
                        .orderByDesc(NewsArticle::getPublishedAt)
                        .orderByDesc(NewsArticle::getId)
                        .last("LIMIT " + limit)
        );

        List<ScoredChunk> results = new ArrayList<>();
        for (NewsArticle article : articles) {
            String cleanTitle = cleanText(article.getTitle());
            String cleanContent = cleanText(joinNewsContent(article));
            if (!StringUtils.hasText(cleanTitle) && !StringUtils.hasText(cleanContent)) {
                continue;
            }
            for (String chunkText : splitIntoChunks(cleanTitle + "\n" + cleanContent)) {
                double score = scoreChunk(query, tokens, cleanTitle, chunkText, article.getPublishedAt(), true);
                if (score <= 0D) {
                    continue;
                }
                results.add(new ScoredChunk(
                        "news:" + article.getId(),
                        article.getId(),
                        "新闻",
                        cleanTitle,
                        "/news/" + article.getId(),
                        buildSnippet(chunkText, tokens),
                        score
                ));
            }
        }
        return results;
    }

    /**
     * 为单个内容块计算启发式相关性分数。
     *
     * @param query 用户原始问题
     * @param tokens 归一化后的检索词
     * @param title 来源文档标题
     * @param chunk 来源内容块文本
     * @param time 用于时效性加权的发布时间或创建时间
     * @param news 当前内容块是否来自新闻
     * @return 相关性分数
     */
    private double scoreChunk(String query, List<String> tokens, String title, String chunk, LocalDateTime time, boolean news) {
        String normalizedQuery = normalizeSearchText(query);
        String normalizedTitle = normalizeSearchText(title);
        String normalizedChunk = normalizeSearchText(chunk);
        double score = 0D;

        if (StringUtils.hasText(normalizedQuery)) {
            if (normalizedTitle.contains(normalizedQuery)) {
                score += 18D;
            }
            if (normalizedChunk.contains(normalizedQuery)) {
                score += 12D;
            }
        }

        for (String token : tokens) {
            if (normalizedTitle.contains(token)) {
                score += token.length() >= 4 ? 6D : 4D;
            }
            if (normalizedChunk.contains(token)) {
                score += token.length() >= 4 ? 3D : 2D;
            }
        }

        if (time != null) {
            long days = Math.max(0, ChronoUnit.DAYS.between(time, LocalDateTime.now()));
            double recencyBoost = news ? Math.max(0D, 6D - days * 0.2D) : Math.max(0D, 3D - days * 0.03D);
            score += recencyBoost;
        }

        return score;
    }

    /**
     * 将新闻中有价值的字段拼接成可检索文本。
     *
     * @param article 新闻实体
     * @return 合并后的文本内容
     */
    private String joinNewsContent(NewsArticle article) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(article.getSummary())) {
            builder.append(article.getSummary()).append("\n");
        }
        if (StringUtils.hasText(article.getContent())) {
            builder.append(article.getContent()).append("\n");
        }
        if (StringUtils.hasText(article.getCategoryName())) {
            builder.append("分类：").append(article.getCategoryName());
        }
        return builder.toString();
    }

    /**
     * 将单篇文档切分为带重叠区域的内容块。
     *
     * @param text 原始文本
     * @return 内容块列表
     */
    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        String clean = cleanText(text);
        if (!StringUtils.hasText(clean)) {
            return chunks;
        }

        int chunkSize = Math.max(180, aiProperties.getRagChunkSize());
        int overlap = Math.max(0, Math.min(aiProperties.getRagChunkOverlap(), chunkSize / 2));
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(clean.length(), start + chunkSize);
            chunks.add(clean.substring(start, end).trim());
            if (end >= clean.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    /**
     * 围绕首个命中的检索词构建摘要片段。
     *
     * @param text 来源内容块文本
     * @param tokens 归一化后的检索词
     * @return 用于前端展示的摘要片段
     */
    private String buildSnippet(String text, List<String> tokens) {
        String clean = cleanText(text);
        if (!StringUtils.hasText(clean)) {
            return "";
        }

        int snippetLength = Math.max(80, aiProperties.getRagSnippetLength());
        String normalized = normalizeSearchText(clean);
        int start = 0;
        for (String token : tokens) {
            int hit = normalized.indexOf(token);
            if (hit >= 0) {
                start = Math.max(0, hit - 30);
                break;
            }
        }
        int end = Math.min(clean.length(), start + snippetLength);
        String snippet = clean.substring(start, end).trim();
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < clean.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    /**
     * 为中英文混合问题生成检索词列表。
     *
     * @param query 用户原始问题
     * @return 归一化后的检索词列表
     */
    private List<String> tokenizeQuery(String query) {
        String normalized = normalizeSearchText(query);
        LinkedHashSet<String> tokens = new LinkedHashSet<>();

        Matcher latinMatcher = LATIN_TOKEN_PATTERN.matcher(normalized);
        while (latinMatcher.find()) {
            tokens.add(latinMatcher.group());
        }

        Matcher cjkMatcher = CJK_SEGMENT_PATTERN.matcher(normalized);
        while (cjkMatcher.find()) {
            String segment = cjkMatcher.group();
            if (segment.length() <= 4) {
                tokens.add(segment);
            }
            for (int index = 0; index < segment.length() - 1; index++) {
                tokens.add(segment.substring(index, index + 2));
            }
        }

        if (tokens.isEmpty() && StringUtils.hasText(normalized)) {
            tokens.add(normalized);
        }
        return new ArrayList<>(tokens);
    }

    /**
     * 对文本进行归一化，便于轻量级词法匹配。
     *
     * @param value 原始文本
     * @return 可用于检索的归一化文本
     */
    private String normalizeSearchText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = cleanText(value).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fff\\s]", " ");
        return MULTI_SPACE_PATTERN.matcher(normalized).replaceAll(" ").trim();
    }

    /**
     * 清理存储内容中的 HTML 和 Markdown 噪声。
     *
     * @param value 原始文本
     * @return 清理后的纯文本
     */
    private String cleanText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String clean = HTML_TAG_PATTERN.matcher(value).replaceAll(" ");
        clean = clean.replaceAll("!\\[[^\\]]*\\]\\([^\\)]*\\)", " ");
        clean = clean.replaceAll("\\[[^\\]]+\\]\\([^\\)]*\\)", " ");
        clean = clean.replaceAll("[#>*`~_-]", " ");
        return MULTI_SPACE_PATTERN.matcher(clean).replaceAll(" ").trim();
    }

    /**
     * 表示一条带分数的检索候选结果。
     */
    private static class ScoredChunk {
        private final String documentKey;
        private final Long documentId;
        private final String typeLabel;
        private final String title;
        private final String url;
        private final String snippet;
        private final double score;

        private ScoredChunk(String documentKey, Long documentId, String typeLabel, String title, String url, String snippet, double score) {
            this.documentKey = documentKey;
            this.documentId = documentId;
            this.typeLabel = typeLabel;
            this.title = title;
            this.url = url;
            this.snippet = snippet;
            this.score = score;
        }

        public String getDocumentKey() {
            return documentKey;
        }

        public Long getDocumentId() {
            return documentId;
        }

        public String getTypeLabel() {
            return typeLabel;
        }

        public String getTitle() {
            return title;
        }

        public String getUrl() {
            return url;
        }

        public String getSnippet() {
            return snippet;
        }

        public double getScore() {
            return score;
        }
    }
}
