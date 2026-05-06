package com.example.blog.controller;

import com.example.blog.dto.NewsRuleUpdateRequest;
import com.example.blog.entity.NewsCategoryRule;
import com.example.blog.service.GuardianNewsFetchService;
import com.example.blog.service.NewsAiSummaryService;
import com.example.blog.service.NewsArticleService;
import com.example.blog.service.NewsCategoryRuleService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.time.LocalDate;

/**
 * 新闻后台管理接口。
 */
@RestController
@RequestMapping("/api/admin/news")
public class NewsAdminController {

    @Resource
    private GuardianNewsFetchService guardianNewsFetchService;

    @Resource
    private NewsCategoryRuleService newsCategoryRuleService;

    @Resource
    private NewsArticleService newsArticleService;

    @Resource
    private NewsAiSummaryService newsAiSummaryService;

    /**
     * 手动触发新闻抓取。
     *
     * @param date 抓取日期
     * @param source 数据源编码，传空表示抓取全部启用数据源
     * @return 抓取结果
     */
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchNews(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String source) {
        return ResponseEntity.ok(guardianNewsFetchService.fetchNews(date, "MANUAL", source));
    }

    /**
     * 分页查询抓取任务日志。
     *
     * @param page 页码
     * @param size 每页数量
     * @return 日志列表
     */
    @GetMapping("/jobs")
    public ResponseEntity<?> getJobLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(guardianNewsFetchService.getJobLogs(page, size));
    }

    /**
     * 查询数据源状态。
     *
     * @return 数据源状态列表
     */
    @GetMapping("/providers")
    public ResponseEntity<?> getProviders() {
        return ResponseEntity.ok(guardianNewsFetchService.getProviderStatuses());
    }

    /**
     * 分页查询重复新闻结果。
     *
     * @param page 页码
     * @param size 每页数量
     * @param date 抓取日期
     * @return 重复新闻结果
     */
    @GetMapping("/duplicates")
    public ResponseEntity<?> getDuplicates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(newsArticleService.getDuplicateArticles(page, size, date));
    }

    /**
     * 查询分类规则列表。
     *
     * @return 规则列表
     */
    @GetMapping("/rules")
    public ResponseEntity<?> getRules() {
        return ResponseEntity.ok(newsCategoryRuleService.getRules());
    }

    /**
     * 手动生成指定日期前十条热度新闻摘要。
     *
     * @param date 新闻抓取日期
     * @param force 是否强制重生成
     * @return 生成结果
     */
    @PostMapping("/summary/generate/top")
    public ResponseEntity<?> generateTopSummaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(newsAiSummaryService.generateTopSummaries(date, "MANUAL_BATCH", force));
    }

    /**
     * 手动生成单条新闻摘要。
     *
     * @param id 新闻主键
     * @param force 是否强制重生成
     * @return 生成结果
     */
    @PostMapping("/{id}/summary")
    public ResponseEntity<?> generateArticleSummary(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.ok(newsAiSummaryService.generateSummary(id, "MANUAL_SINGLE", force));
    }

    /**
     * 查询每日摘要额度。
     *
     * @param date 统计日期
     * @return 额度信息
     */
    @GetMapping("/summary/quota")
    public ResponseEntity<?> getSummaryQuota(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(newsAiSummaryService.getDailyQuota(date));
    }

    /**
     * 更新分类规则。
     *
     * @param id 规则主键
     * @param request 更新请求
     * @return 更新结果
     */
    @PutMapping("/rules/{id}")
    public ResponseEntity<?> updateRule(@PathVariable Long id, @RequestBody NewsRuleUpdateRequest request) {
        NewsCategoryRule rule = newsCategoryRuleService.updateRule(id, request);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }
}
