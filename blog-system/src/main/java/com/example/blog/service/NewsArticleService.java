package com.example.blog.service;

import com.example.blog.entity.NewsArticle;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 新闻浏览服务。
 */
public interface NewsArticleService {
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
    Map<String, Object> getNewsPage(int page, int size, String category, String keyword, LocalDate fetchDate);

    /**
     * 查询指定日期的 Top 新闻。
     *
     * @param fetchDate 抓取日期
     * @param category 分类编码
     * @param limit 返回数量
     * @return Top 新闻结果
     */
    Map<String, Object> getTopNews(LocalDate fetchDate, String category, int limit);

    /**
     * 查询新闻详情。
     *
     * @param id 新闻主键
     * @return 新闻详情
     */
    NewsArticle getNewsDetail(Long id);

    /**
     * 查询分类统计。
     *
     * @param fetchDate 抓取日期
     * @return 分类统计列表
     */
    List<Map<String, Object>> getCategories(LocalDate fetchDate);

    /**
     * 查询最近一次抓取日期。
     *
     * @return 最近抓取日期
     */
    LocalDate getLatestFetchDate();
}
