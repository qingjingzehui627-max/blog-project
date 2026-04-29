package com.example.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.blog.dto.NewsRuleUpdateRequest;
import com.example.blog.entity.NewsCategoryRule;
import com.example.blog.mapper.NewsCategoryRuleMapper;
import com.example.blog.service.NewsCategoryRuleService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 新闻分类规则服务实现。
 */
@Service
public class NewsCategoryRuleServiceImpl extends ServiceImpl<NewsCategoryRuleMapper, NewsCategoryRule> implements NewsCategoryRuleService {

    /**
     * 查询全部分类规则。
     *
     * @return 规则列表
     */
    @Override
    public java.util.List<NewsCategoryRule> getRules() {
        return baseMapper.selectList(
                new LambdaQueryWrapper<NewsCategoryRule>()
                        .orderByAsc(NewsCategoryRule::getPriority)
                        .orderByAsc(NewsCategoryRule::getId)
        );
    }

    /**
     * 更新分类规则。
     *
     * @param id 规则主键
     * @param request 更新请求
     * @return 更新后的规则
     */
    @Override
    public NewsCategoryRule updateRule(Long id, NewsRuleUpdateRequest request) {
        NewsCategoryRule rule = baseMapper.selectById(id);
        if (rule == null) {
            return null;
        }
        if (StringUtils.hasText(request.getCategoryCode())) {
            rule.setCategoryCode(request.getCategoryCode().trim());
        }
        if (StringUtils.hasText(request.getCategoryName())) {
            rule.setCategoryName(request.getCategoryName().trim());
        }
        if (request.getIncludeKeywords() != null) {
            rule.setIncludeKeywords(request.getIncludeKeywords().trim());
        }
        if (request.getExcludeKeywords() != null) {
            rule.setExcludeKeywords(request.getExcludeKeywords().trim());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        baseMapper.updateById(rule);
        return baseMapper.selectById(id);
    }
}
