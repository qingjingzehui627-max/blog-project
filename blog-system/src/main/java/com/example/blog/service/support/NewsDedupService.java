package com.example.blog.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.NewsArticleSource;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.mapper.NewsArticleSourceMapper;
import com.example.blog.service.support.dto.NewsDedupResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 新闻去重服务。
 */
@Service
public class NewsDedupService {

    private static final String STAGE_SOURCE = "SOURCE_ID";
    private static final String STAGE_URL = "URL_HASH";
    private static final String STAGE_TITLE = "TITLE_MATCH";
    private static final double TITLE_SIMILARITY_THRESHOLD = 0.9D;

    @Resource
    private NewsArticleMapper newsArticleMapper;

    @Resource
    private NewsArticleSourceMapper newsArticleSourceMapper;

    @Resource
    private NewsContentNormalizer newsContentNormalizer;

    /**
     * 执行三段去重匹配。
     *
     * @param candidate 待匹配新闻
     * @param providerCode 数据源编码
     * @param sourceContentId 来源内容 ID
     * @param fetchDate 抓取日期
     * @return 去重匹配结果
     */
    public NewsDedupResult findDuplicate(
            NewsArticle candidate,
            String providerCode,
            String sourceContentId,
            LocalDate fetchDate) {
        NewsDedupResult sourceMatch = matchBySourceRecord(providerCode, sourceContentId, fetchDate);
        if (sourceMatch.isMatched()) {
            return sourceMatch;
        }

        NewsDedupResult urlMatch = matchByUrlHash(candidate, fetchDate);
        if (urlMatch.isMatched()) {
            return urlMatch;
        }

        return matchByTitle(candidate, fetchDate);
    }

    /**
     * 第一段：按数据源自身主键去重。
     *
     * @param providerCode 数据源编码
     * @param sourceContentId 来源内容 ID
     * @param fetchDate 抓取日期
     * @return 去重匹配结果
     */
    private NewsDedupResult matchBySourceRecord(String providerCode, String sourceContentId, LocalDate fetchDate) {
        NewsDedupResult result = new NewsDedupResult();
        if (!StringUtils.hasText(providerCode) || !StringUtils.hasText(sourceContentId) || fetchDate == null) {
            return result;
        }

        NewsArticleSource sourceRecord = newsArticleSourceMapper.selectOne(
                new LambdaQueryWrapper<NewsArticleSource>()
                        .eq(NewsArticleSource::getProviderCode, providerCode)
                        .eq(NewsArticleSource::getSourceContentId, sourceContentId)
                        .eq(NewsArticleSource::getFetchDate, fetchDate)
                        .last("LIMIT 1")
        );
        if (sourceRecord == null) {
            return result;
        }

        NewsArticle article = newsArticleMapper.selectById(sourceRecord.getArticleId());
        if (article == null) {
            return result;
        }

        result.setMatched(true);
        result.setStage(STAGE_SOURCE);
        result.setScore(1D);
        result.setArticle(article);
        result.setSourceRecord(sourceRecord);
        return result;
    }

    /**
     * 第二段：按规范化 URL 精确去重。
     *
     * @param candidate 待匹配新闻
     * @param fetchDate 抓取日期
     * @return 去重匹配结果
     */
    private NewsDedupResult matchByUrlHash(NewsArticle candidate, LocalDate fetchDate) {
        NewsDedupResult result = new NewsDedupResult();
        if (!StringUtils.hasText(candidate.getUrlHash()) || fetchDate == null) {
            return result;
        }

        NewsArticle article = newsArticleMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getFetchDate, fetchDate)
                        .eq(NewsArticle::getUrlHash, candidate.getUrlHash())
                        .last("LIMIT 1")
        );
        if (article == null) {
            return result;
        }

        result.setMatched(true);
        result.setStage(STAGE_URL);
        result.setScore(1D);
        result.setArticle(article);
        return result;
    }

    /**
     * 第三段：按标题精确匹配和相似匹配去重。
     *
     * @param candidate 待匹配新闻
     * @param fetchDate 抓取日期
     * @return 去重匹配结果
     */
    private NewsDedupResult matchByTitle(NewsArticle candidate, LocalDate fetchDate) {
        NewsDedupResult result = new NewsDedupResult();
        if (!StringUtils.hasText(candidate.getNormalizedTitle()) || candidate.getPublishedAt() == null || fetchDate == null) {
            return result;
        }

        LocalDateTime startTime = candidate.getPublishedAt().minusHours(24);
        LocalDateTime endTime = candidate.getPublishedAt().plusHours(24);
        List<NewsArticle> candidates = newsArticleMapper.selectList(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getFetchDate, fetchDate)
                        .between(NewsArticle::getPublishedAt, startTime, endTime)
                        .orderByDesc(NewsArticle::getPublishedAt)
                        .last("LIMIT 30")
        );
        if (candidates.isEmpty()) {
            return result;
        }

        // 先走标题哈希的精确匹配，只有未命中时才进入相似度匹配，
        // 这样可以优先命中确定性更高的重复结果。
        if (StringUtils.hasText(candidate.getTitleHash())) {
            NewsArticle exactMatch = candidates.stream()
                    .filter(item -> candidate.getTitleHash().equals(item.getTitleHash()))
                    .findFirst()
                    .orElse(null);
            if (exactMatch != null) {
                result.setMatched(true);
                result.setStage(STAGE_TITLE);
                result.setScore(1D);
                result.setArticle(exactMatch);
                return result;
            }
        }

        NewsArticle bestMatch = candidates.stream()
                .max(Comparator.comparingDouble(item ->
                        newsContentNormalizer.calculateTitleSimilarity(candidate.getTitle(), item.getTitle())))
                .orElse(null);
        if (bestMatch == null) {
            return result;
        }

        double similarity = newsContentNormalizer.calculateTitleSimilarity(candidate.getTitle(), bestMatch.getTitle());
        if (similarity < TITLE_SIMILARITY_THRESHOLD) {
            return result;
        }

        result.setMatched(true);
        result.setStage(STAGE_TITLE);
        result.setScore(similarity);
        result.setArticle(bestMatch);
        return result;
    }
}
