package com.example.blog.controller;

import com.example.blog.dto.NewsRuleUpdateRequest;
import com.example.blog.entity.NewsCategoryRule;
import com.example.blog.service.GuardianNewsFetchService;
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

    /**
     * 手动触发新闻抓取。
     *
     * @param date 抓取日期
     * @return 抓取结果
     */
    @PostMapping("/fetch")
    public ResponseEntity<?> fetchNews(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(guardianNewsFetchService.fetchNews(date, "MANUAL"));
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
     * 查询分类规则列表。
     *
     * @return 规则列表
     */
    @GetMapping("/rules")
    public ResponseEntity<?> getRules() {
        return ResponseEntity.ok(newsCategoryRuleService.getRules());
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
