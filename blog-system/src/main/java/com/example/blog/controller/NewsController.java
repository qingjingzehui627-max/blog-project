package com.example.blog.controller;

import com.example.blog.entity.NewsArticle;
import com.example.blog.service.NewsArticleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 新闻公开浏览接口。
 */
@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Resource
    private NewsArticleService newsArticleService;

    /**
     * 分页查询新闻列表。
     *
     * @param page 页码
     * @param size 每页数量
     * @param category 分类编码
     * @param keyword 搜索关键词
     * @param date 抓取日期
     * @return 新闻列表
     */
    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(newsArticleService.getNewsPage(page, size, category, keyword, date));
    }

    /**
     * 查询指定日期的 Top 新闻。
     *
     * @param category 分类编码
     * @param limit 返回数量
     * @param date 抓取日期
     * @return Top 新闻列表
     */
    @GetMapping("/top")
    public ResponseEntity<?> getTopNews(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(newsArticleService.getTopNews(date, category, limit));
    }

    /**
     * 查询分类统计。
     *
     * @param date 抓取日期
     * @return 分类统计
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getCategories(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(newsArticleService.getCategories(date));
    }

    /**
     * 查询新闻详情。
     *
     * @param id 新闻主键
     * @return 新闻详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getNewsDetail(@PathVariable Long id) {
        NewsArticle article = newsArticleService.getNewsDetail(id);
        if (article == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(article);
    }
}
