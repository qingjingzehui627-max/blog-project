package com.example.blog.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.classifier.NewsClassifier;
import com.example.blog.client.provider.dto.NewsProviderArticle;
import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.NewsArticleSource;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.mapper.NewsArticleSourceMapper;
import com.example.blog.service.support.dto.NewsAggregationResult;
import com.example.blog.service.support.dto.NewsDedupResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 新闻聚合写入服务。
 */
@Service
public class NewsAggregationService {

    @Resource
    private NewsArticleMapper newsArticleMapper;

    @Resource
    private NewsArticleSourceMapper newsArticleSourceMapper;

    @Resource
    private NewsClassifier newsClassifier;

    @Resource
    private NewsContentNormalizer newsContentNormalizer;

    @Resource
    private NewsDedupService newsDedupService;

    /**
     * 聚合并写入单个数据源的新闻结果。
     *
     * @param providerCode 数据源编码
     * @param providerArticles 数据源新闻列表
     * @param fetchDate 抓取日期
     * @param topLimit 每日保留的最大新闻数量
     * @return 聚合写入结果
     */
    public NewsAggregationResult aggregateArticles(
            String providerCode,
            List<NewsProviderArticle> providerArticles,
            LocalDate fetchDate,
            int topLimit) {
        NewsAggregationResult result = new NewsAggregationResult();
        if (providerArticles == null || providerArticles.isEmpty()) {
            refreshDailyRanking(fetchDate, topLimit);
            return result;
        }

        for (NewsProviderArticle providerArticle : providerArticles) {
            if (!isValid(providerArticle)) {
                result.setSkippedCount(result.getSkippedCount() + 1);
                continue;
            }

            result.setFetchedCount(result.getFetchedCount() + 1);
            NewsArticle candidate = buildCandidateArticle(providerArticle, fetchDate);
            NewsDedupResult dedupResult = newsDedupService.findDuplicate(
                    candidate,
                    providerCode,
                    providerArticle.getSourceContentId(),
                    fetchDate
            );

            if (!dedupResult.isMatched()) {
                insertNewArticle(candidate, providerArticle, fetchDate);
                result.setInsertedCount(result.getInsertedCount() + 1);
                continue;
            }

            // 命中重复后不新增主新闻，只把更完整的内容合并到已有聚合记录里。
            updateExistingArticle(dedupResult, candidate, providerArticle, fetchDate);
            result.setUpdatedCount(result.getUpdatedCount() + 1);
        }

        refreshDailyRanking(fetchDate, topLimit);
        return result;
    }

    /**
     * 构建聚合主表候选新闻。
     *
     * @param providerArticle 来源新闻
     * @param fetchDate 抓取日期
     * @return 聚合主表候选新闻
     */
    private NewsArticle buildCandidateArticle(NewsProviderArticle providerArticle, LocalDate fetchDate) {
        NewsArticle article = new NewsArticle();
        article.setSource(providerArticle.getProviderCode());
        article.setSourceContentId(limit(providerArticle.getSourceContentId(), 255));
        article.setPrimaryProvider(providerArticle.getProviderCode());
        article.setSectionId(limit(providerArticle.getSectionId(), 100));
        article.setSectionName(limit(providerArticle.getSectionName(), 100));
        article.setPillarId(limit(providerArticle.getPillarId(), 100));
        article.setPillarName(limit(providerArticle.getPillarName(), 100));
        article.setTitle(limit(providerArticle.getTitle(), 500));
        article.setNormalizedTitle(limit(newsContentNormalizer.normalizeTitle(providerArticle.getTitle()), 500));
        article.setTitleHash(limit(newsContentNormalizer.hash(article.getNormalizedTitle()), 64));
        article.setSummary(limit(providerArticle.getSummary(), 5000));
        article.setContent(providerArticle.getContent());
        article.setAuthor(limit(providerArticle.getAuthor(), 255));
        article.setWebUrl(limit(providerArticle.getWebUrl(), 500));
        article.setNormalizedUrl(limit(newsContentNormalizer.normalizeUrl(providerArticle.getWebUrl()), 500));
        article.setUrlHash(limit(newsContentNormalizer.hash(article.getNormalizedUrl()), 64));
        article.setApiUrl(limit(providerArticle.getApiUrl(), 500));
        article.setThumbnailUrl(limit(providerArticle.getThumbnailUrl(), 500));
        article.setPublishedAt(providerArticle.getPublishedAt() != null ? providerArticle.getPublishedAt() : fetchDate.atStartOfDay());
        article.setFetchDate(fetchDate);
        article.setSourceCount(1);
        article.setLang(StringUtils.hasText(providerArticle.getLang()) ? providerArticle.getLang() : "en");
        article.setStatus(1);
        article.setRawJson(providerArticle.getRawJson());

        NewsClassifier.ClassificationResult classificationResult = newsClassifier.classify(
                article.getTitle(),
                article.getSummary(),
                article.getContent()
        );
        article.setCategoryCode(classificationResult.getCategoryCode());
        article.setCategoryName(classificationResult.getCategoryName());
        article.setKeywordTags(limit(classificationResult.getMatchedKeywords(), 500));
        return article;
    }

    /**
     * 新增一条聚合新闻并写入来源明细。
     *
     * @param article 聚合新闻
     * @param providerArticle 来源新闻
     * @param fetchDate 抓取日期
     */
    private void insertNewArticle(NewsArticle article, NewsProviderArticle providerArticle, LocalDate fetchDate) {
        LocalDateTime now = LocalDateTime.now();
        article.setCreatedAt(now);
        article.setUpdatedAt(now);
        newsArticleMapper.insert(article);
        insertSourceRecord(article.getId(), providerArticle, fetchDate);
    }

    /**
     * 合并已有聚合新闻并更新来源明细。
     *
     * @param dedupResult 去重结果
     * @param candidate 待合并新闻
     * @param providerArticle 来源新闻
     * @param fetchDate 抓取日期
     */
    private void updateExistingArticle(
            NewsDedupResult dedupResult,
            NewsArticle candidate,
            NewsProviderArticle providerArticle,
            LocalDate fetchDate) {
        NewsArticle existingArticle = dedupResult.getArticle();
        NewsArticleSource sourceRecord = dedupResult.getSourceRecord();

        mergeArticle(existingArticle, candidate, sourceRecord == null);
        newsArticleMapper.updateById(existingArticle);

        if (sourceRecord == null) {
            insertSourceRecord(existingArticle.getId(), providerArticle, fetchDate);
        } else {
            updateSourceRecord(sourceRecord, providerArticle);
        }
    }

    /**
     * 合并两条新闻内容。
     *
     * @param existingArticle 已存在新闻
     * @param candidate 待合并新闻
     * @param increaseSourceCount 是否增加来源数量
     */
    private void mergeArticle(NewsArticle existingArticle, NewsArticle candidate, boolean increaseSourceCount) {
        // 去重命中后保留更完整的标题、摘要和正文，尽量让聚合主表展示效果更好。
        existingArticle.setTitle(selectLongerText(existingArticle.getTitle(), candidate.getTitle()));
        existingArticle.setNormalizedTitle(selectLongerText(existingArticle.getNormalizedTitle(), candidate.getNormalizedTitle()));
        existingArticle.setTitleHash(selectPreferredValue(existingArticle.getTitleHash(), candidate.getTitleHash()));
        existingArticle.setSummary(selectLongerText(existingArticle.getSummary(), candidate.getSummary()));
        existingArticle.setContent(selectLongerText(existingArticle.getContent(), candidate.getContent()));
        existingArticle.setAuthor(selectPreferredValue(existingArticle.getAuthor(), candidate.getAuthor()));
        existingArticle.setWebUrl(selectPreferredValue(existingArticle.getWebUrl(), candidate.getWebUrl()));
        existingArticle.setNormalizedUrl(selectPreferredValue(existingArticle.getNormalizedUrl(), candidate.getNormalizedUrl()));
        existingArticle.setUrlHash(selectPreferredValue(existingArticle.getUrlHash(), candidate.getUrlHash()));
        existingArticle.setApiUrl(selectPreferredValue(existingArticle.getApiUrl(), candidate.getApiUrl()));
        existingArticle.setThumbnailUrl(selectPreferredValue(existingArticle.getThumbnailUrl(), candidate.getThumbnailUrl()));
        existingArticle.setSectionId(selectPreferredValue(existingArticle.getSectionId(), candidate.getSectionId()));
        existingArticle.setSectionName(selectPreferredValue(existingArticle.getSectionName(), candidate.getSectionName()));
        existingArticle.setPillarId(selectPreferredValue(existingArticle.getPillarId(), candidate.getPillarId()));
        existingArticle.setPillarName(selectPreferredValue(existingArticle.getPillarName(), candidate.getPillarName()));
        existingArticle.setCategoryCode(selectPreferredValue(existingArticle.getCategoryCode(), candidate.getCategoryCode()));
        existingArticle.setCategoryName(selectPreferredValue(existingArticle.getCategoryName(), candidate.getCategoryName()));
        existingArticle.setKeywordTags(selectLongerText(existingArticle.getKeywordTags(), candidate.getKeywordTags()));
        existingArticle.setRawJson(selectLongerText(existingArticle.getRawJson(), candidate.getRawJson()));
        existingArticle.setPublishedAt(selectEarlierTime(existingArticle.getPublishedAt(), candidate.getPublishedAt()));
        existingArticle.setUpdatedAt(LocalDateTime.now());
        if (increaseSourceCount) {
            existingArticle.setSourceCount((existingArticle.getSourceCount() == null ? 0 : existingArticle.getSourceCount()) + 1);
        }
    }

    /**
     * 插入来源明细记录。
     *
     * @param articleId 聚合新闻主键
     * @param providerArticle 来源新闻
     * @param fetchDate 抓取日期
     */
    private void insertSourceRecord(Long articleId, NewsProviderArticle providerArticle, LocalDate fetchDate) {
        NewsArticleSource sourceRecord = new NewsArticleSource();
        sourceRecord.setArticleId(articleId);
        sourceRecord.setProviderCode(providerArticle.getProviderCode());
        sourceRecord.setSourceContentId(limit(providerArticle.getSourceContentId(), 255));
        sourceRecord.setSourceUrl(limit(providerArticle.getWebUrl(), 500));
        sourceRecord.setNormalizedUrl(limit(newsContentNormalizer.normalizeUrl(providerArticle.getWebUrl()), 500));
        sourceRecord.setUrlHash(limit(newsContentNormalizer.hash(sourceRecord.getNormalizedUrl()), 64));
        sourceRecord.setTitle(limit(providerArticle.getTitle(), 500));
        sourceRecord.setNormalizedTitle(limit(newsContentNormalizer.normalizeTitle(providerArticle.getTitle()), 500));
        sourceRecord.setTitleHash(limit(newsContentNormalizer.hash(sourceRecord.getNormalizedTitle()), 64));
        sourceRecord.setPublishedAt(providerArticle.getPublishedAt() != null ? providerArticle.getPublishedAt() : fetchDate.atStartOfDay());
        sourceRecord.setFetchDate(fetchDate);
        sourceRecord.setRawJson(providerArticle.getRawJson());
        sourceRecord.setCreatedAt(LocalDateTime.now());
        sourceRecord.setUpdatedAt(LocalDateTime.now());
        newsArticleSourceMapper.insert(sourceRecord);
    }

    /**
     * 更新来源明细记录。
     *
     * @param sourceRecord 旧来源记录
     * @param providerArticle 新来源新闻
     */
    private void updateSourceRecord(NewsArticleSource sourceRecord, NewsProviderArticle providerArticle) {
        sourceRecord.setSourceUrl(limit(providerArticle.getWebUrl(), 500));
        sourceRecord.setNormalizedUrl(limit(newsContentNormalizer.normalizeUrl(providerArticle.getWebUrl()), 500));
        sourceRecord.setUrlHash(limit(newsContentNormalizer.hash(sourceRecord.getNormalizedUrl()), 64));
        sourceRecord.setTitle(limit(providerArticle.getTitle(), 500));
        sourceRecord.setNormalizedTitle(limit(newsContentNormalizer.normalizeTitle(providerArticle.getTitle()), 500));
        sourceRecord.setTitleHash(limit(newsContentNormalizer.hash(sourceRecord.getNormalizedTitle()), 64));
        sourceRecord.setPublishedAt(providerArticle.getPublishedAt());
        sourceRecord.setRawJson(providerArticle.getRawJson());
        sourceRecord.setUpdatedAt(LocalDateTime.now());
        newsArticleSourceMapper.updateById(sourceRecord);
    }

    /**
     * 刷新指定日期的展示排序。
     *
     * @param fetchDate 抓取日期
     * @param topLimit 每日保留的最大新闻数量
     */
    private void refreshDailyRanking(LocalDate fetchDate, int topLimit) {
        List<NewsArticle> articles = newsArticleMapper.selectList(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getFetchDate, fetchDate)
                        .orderByDesc(NewsArticle::getId)
        );
        if (articles.isEmpty()) {
            return;
        }

        syncSourceCount(articles);
        articles.sort(Comparator
                .comparing(NewsArticle::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NewsArticle::getSourceCount, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(NewsArticle::getId, Comparator.nullsLast(Comparator.reverseOrder())));

        for (int index = 0; index < articles.size(); index++) {
            NewsArticle article = articles.get(index);
            // 每次抓取结束后重新计算当日展示顺序，只保留 Top N 为可见状态。
            if (index < topLimit) {
                article.setRankOrder(index + 1);
                article.setStatus(1);
            } else {
                article.setRankOrder(null);
                article.setStatus(0);
            }
            article.setUpdatedAt(LocalDateTime.now());
            newsArticleMapper.updateById(article);
        }
    }

    /**
     * 根据来源明细表回填真实来源数量，避免历史数据的计数偏差影响排序和去重展示。
     *
     * @param articles 同一天的聚合新闻列表
     */
    private void syncSourceCount(List<NewsArticle> articles) {
        List<Long> articleIds = articles.stream()
                .map(NewsArticle::getId)
                .filter(id -> id != null)
                .toList();
        if (articleIds.isEmpty()) {
            return;
        }

        Map<Long, Long> sourceCountMap = newsArticleSourceMapper.selectList(
                new LambdaQueryWrapper<NewsArticleSource>()
                        .in(NewsArticleSource::getArticleId, articleIds)
        ).stream().collect(Collectors.groupingBy(NewsArticleSource::getArticleId, Collectors.counting()));

        for (NewsArticle article : articles) {
            article.setSourceCount(sourceCountMap.getOrDefault(article.getId(), 0L).intValue());
        }
    }

    /**
     * 判断来源新闻是否有效。
     *
     * @param providerArticle 来源新闻
     * @return true 表示有效
     */
    private boolean isValid(NewsProviderArticle providerArticle) {
        return providerArticle != null
                && StringUtils.hasText(providerArticle.getSourceContentId())
                && StringUtils.hasText(providerArticle.getTitle())
                && StringUtils.hasText(providerArticle.getWebUrl());
    }

    /**
     * 选择更优的文本内容。
     *
     * @param current 旧值
     * @param incoming 新值
     * @return 优先保留更完整的文本
     */
    private String selectLongerText(String current, String incoming) {
        if (!StringUtils.hasText(current)) {
            return incoming;
        }
        if (!StringUtils.hasText(incoming)) {
            return current;
        }
        return incoming.length() > current.length() ? incoming : current;
    }

    /**
     * 选择优先值。
     *
     * @param current 旧值
     * @param incoming 新值
     * @return 非空优先值
     */
    private String selectPreferredValue(String current, String incoming) {
        return StringUtils.hasText(current) ? current : incoming;
    }

    /**
     * 选择更早的发布时间。
     *
     * @param current 旧时间
     * @param incoming 新时间
     * @return 更早的发布时间
     */
    private LocalDateTime selectEarlierTime(LocalDateTime current, LocalDateTime incoming) {
        if (current == null) {
            return incoming;
        }
        if (incoming == null) {
            return current;
        }
        return incoming.isBefore(current) ? incoming : current;
    }

    /**
     * 截断超长字符串。
     *
     * @param value 原始值
     * @param maxLength 最大长度
     * @return 截断后的值
     */
    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
