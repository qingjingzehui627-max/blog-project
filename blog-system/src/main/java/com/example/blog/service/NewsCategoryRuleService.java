package com.example.blog.service;

import com.example.blog.dto.NewsRuleUpdateRequest;
import com.example.blog.entity.NewsCategoryRule;

import java.util.List;

/**
 * 新闻分类规则服务。
 */
public interface NewsCategoryRuleService {
    /**
     * 查询全部分类规则。
     *
     * @return 规则列表
     */
    List<NewsCategoryRule> getRules();

    /**
     * 更新分类规则。
     *
     * @param id 规则主键
     * @param request 更新请求
     * @return 更新后的规则
     */
    NewsCategoryRule updateRule(Long id, NewsRuleUpdateRequest request);
}
