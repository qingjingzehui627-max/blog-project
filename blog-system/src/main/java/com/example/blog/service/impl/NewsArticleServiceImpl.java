package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.dto.NewsDuplicateArticleResponse;
import com.example.blog.dto.NewsDuplicateSourceResponse;
import com.example.blog.entity.NewsArticle;
import com.example.blog.entity.NewsArticleSource;
import com.example.blog.mapper.NewsArticleMapper;
import com.example.blog.mapper.NewsArticleSourceMapper;
import com.example.blog.service.NewsArticleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 新闻浏览服务实现。
 */
@Service
public class NewsArticleServiceImpl extends ServiceImpl<NewsArticleMapper, NewsArticle> implements NewsArticleService {

    @Resource
    private NewsArticleSourceMapper newsArticleSourceMapper;

    /**
     * 分页查询新闻列表。
     *
     * @param page 页码
     * @param size 每页数量
     * @param category 分类编码
     * @param keyword 搜索关键词
     * @param fetchDate 抓取日期
     * @return 列表结果
     */
    @Override
    public Map<String, Object> getNewsPage(int page, int size, String category, String keyword, LocalDate fetchDate) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        LocalDate targetDate = fetchDate != null ? fetchDate : getLatestFetchDate();

        LambdaQueryWrapper<NewsArticle> countWrapper = buildListWrapper(category, keyword, targetDate);
        Long total = baseMapper.selectCount(countWrapper);

        LambdaQueryWrapper<NewsArticle> listWrapper = buildListWrapper(category, keyword, targetDate)
                .last("LIMIT " + (safePage - 1) * safeSize + ", " + safeSize);
        List<NewsArticle> list = baseMapper.selectList(listWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", total);
        result.put("fetchDate", targetDate);
        return result;
    }

    /**
     * 查询指定日期的 Top 新闻。
     *
     * @param fetchDate 抓取日期
     * @param category 分类编码
     * @param limit 返回数量
     * @return Top 新闻结果
     */
    @Override
    public Map<String, Object> getTopNews(LocalDate fetchDate, String category, int limit) {
        int safeLimit = Math.max(limit, 1);
        LocalDate targetDate = fetchDate != null ? fetchDate : getLatestFetchDate();

        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1);
        if (targetDate != null) {
            wrapper.eq(NewsArticle::getFetchDate, targetDate);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(NewsArticle::getCategoryCode, category);
        }
        List<NewsArticle> list = baseMapper.selectList(
                wrapper.orderByAsc(NewsArticle::getRankOrder)
                        .orderByDesc(NewsArticle::getPublishedAt)
                        .last("LIMIT " + safeLimit)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("fetchDate", targetDate);
        result.put("limit", safeLimit);
        return result;
    }

    /**
     * 查询新闻详情。
     *
     * @param id 新闻主键
     * @return 新闻详情
     */
    @Override
    public NewsArticle getNewsDetail(Long id) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getId, id)
                        .eq(NewsArticle::getStatus, 1)
                        .last("LIMIT 1")
        );
    }

    /**
     * 查询分类统计。
     *
     * @param fetchDate 抓取日期
     * @return 分类统计列表
     */
    @Override
    public List<Map<String, Object>> getCategories(LocalDate fetchDate) {
        LocalDate targetDate = fetchDate != null ? fetchDate : getLatestFetchDate();
        QueryWrapper<NewsArticle> wrapper = new QueryWrapper<>();
        wrapper.select(
                        "category_code AS categoryCode",
                        "category_name AS categoryName",
                        "COUNT(*) AS articleCount",
                        "MIN(rank_order) AS minRank"
                )
                .eq("status", 1);
        if (targetDate != null) {
            wrapper.eq("fetch_date", targetDate);
        }
        wrapper.groupBy("category_code", "category_name")
                .orderByAsc("minRank")
                .orderByDesc("articleCount");
        return baseMapper.selectMaps(wrapper);
    }

    /**
     * 查询最近一次抓取日期。
     *
     * @return 最近抓取日期
     */
    @Override
    public LocalDate getLatestFetchDate() {
        NewsArticle latestArticle = baseMapper.selectOne(
                new LambdaQueryWrapper<NewsArticle>()
                        .eq(NewsArticle::getStatus, 1)
                        .orderByDesc(NewsArticle::getFetchDate)
                        .orderByAsc(NewsArticle::getRankOrder)
                        .last("LIMIT 1")
        );
        return latestArticle == null ? null : latestArticle.getFetchDate();
    }

    /**
     * 分页查询重复新闻结果。
     *
     * @param page 页码
     * @param size 每页数量
     * @param fetchDate 抓取日期
     * @return 重复新闻结果
     */
    @Override
    public Map<String, Object> getDuplicateArticles(int page, int size, LocalDate fetchDate) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);
        LocalDate targetDate = fetchDate != null ? fetchDate : getLatestFetchDate();

        LambdaQueryWrapper<NewsArticle> articleWrapper = new LambdaQueryWrapper<>();
        if (targetDate != null) {
            articleWrapper.eq(NewsArticle::getFetchDate, targetDate);
        }
        List<NewsArticle> articles = baseMapper.selectList(articleWrapper);

        List<Long> articleIds = articles.stream().map(NewsArticle::getId).toList();
        Map<Long, List<NewsArticleSource>> sourceMap = articleIds.isEmpty()
                ? Map.of()
                : newsArticleSourceMapper.selectList(
                        new LambdaQueryWrapper<NewsArticleSource>()
                                .in(NewsArticleSource::getArticleId, articleIds)
                                .orderByAsc(NewsArticleSource::getProviderCode)
                                .orderByDesc(NewsArticleSource::getPublishedAt)
                ).stream().collect(Collectors.groupingBy(NewsArticleSource::getArticleId));

        List<NewsDuplicateArticleResponse> duplicateArticles = articles.stream()
                .map(article -> toDuplicateResponse(article, sourceMap.get(article.getId())))
                .filter(article -> article.getSourceCount() != null && article.getSourceCount() > 1)
                .sorted(Comparator
                        .comparing(NewsDuplicateArticleResponse::getSourceCount, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NewsDuplicateArticleResponse::getPublishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(NewsDuplicateArticleResponse::getArticleId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        int fromIndex = Math.min((safePage - 1) * safeSize, duplicateArticles.size());
        int toIndex = Math.min(fromIndex + safeSize, duplicateArticles.size());
        List<NewsDuplicateArticleResponse> list = duplicateArticles.subList(fromIndex, toIndex);

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("total", duplicateArticles.size());
        result.put("fetchDate", targetDate);
        return result;
    }

    /**
     * 构造新闻列表查询条件。
     *
     * @param category 分类编码
     * @param keyword 搜索关键词
     * @param fetchDate 抓取日期
     * @return 查询条件
     */
    private LambdaQueryWrapper<NewsArticle> buildListWrapper(String category, String keyword, LocalDate fetchDate) {
        LambdaQueryWrapper<NewsArticle> wrapper = new LambdaQueryWrapper<NewsArticle>()
                .eq(NewsArticle::getStatus, 1);

        if (fetchDate != null) {
            wrapper.eq(NewsArticle::getFetchDate, fetchDate);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(NewsArticle::getCategoryCode, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(query -> query
                    .like(NewsArticle::getTitle, keyword)
                    .or()
                    .like(NewsArticle::getSummary, keyword)
                    .or()
                    .like(NewsArticle::getContent, keyword));
        }
        return wrapper.orderByAsc(NewsArticle::getRankOrder)
                .orderByDesc(NewsArticle::getPublishedAt)
                .orderByDesc(NewsArticle::getId);
    }

    /**
     * 转换重复新闻响应结构。
     *
     * @param article 聚合新闻
     * @param sources 来源明细
     * @return 重复新闻响应
     */
    private NewsDuplicateArticleResponse toDuplicateResponse(NewsArticle article, List<NewsArticleSource> sources) {
        List<NewsDuplicateSourceResponse> sourceResponses = (sources == null ? List.<NewsArticleSource>of() : sources).stream()
                .map(this::toDuplicateSourceResponse)
                .toList();
        NewsDuplicateArticleResponse response = new NewsDuplicateArticleResponse();
        response.setArticleId(article.getId());
        response.setTitle(article.getTitle());
        response.setWebUrl(article.getWebUrl());
        response.setCategoryCode(article.getCategoryCode());
        response.setCategoryName(article.getCategoryName());
        response.setPrimaryProvider(article.getPrimaryProvider());
        response.setSourceCount(sourceResponses.size());
        response.setFetchDate(article.getFetchDate());
        response.setPublishedAt(article.getPublishedAt());
        response.setSources(sourceResponses);
        return response;
    }

    /**
     * 转换来源明细响应结构。
     *
     * @param source 来源明细
     * @return 来源明细响应
     */
    private NewsDuplicateSourceResponse toDuplicateSourceResponse(NewsArticleSource source) {
        NewsDuplicateSourceResponse response = new NewsDuplicateSourceResponse();
        response.setId(source.getId());
        response.setProviderCode(source.getProviderCode());
        response.setSourceContentId(source.getSourceContentId());
        response.setTitle(source.getTitle());
        response.setSourceUrl(source.getSourceUrl());
        response.setPublishedAt(source.getPublishedAt());
        response.setFetchDate(source.getFetchDate());
        return response;
    }
}
