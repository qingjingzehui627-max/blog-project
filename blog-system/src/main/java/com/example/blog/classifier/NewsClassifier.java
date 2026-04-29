package com.example.blog.classifier;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.blog.config.GuardianProperties;
import com.example.blog.entity.NewsCategoryRule;
import com.example.blog.mapper.NewsCategoryRuleMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 新闻关键词规则分类器。
 */
@Component
public class NewsClassifier {

    @Resource
    private NewsCategoryRuleMapper newsCategoryRuleMapper;

    @Resource
    private GuardianProperties guardianProperties;

    /**
     * 根据标题、摘要和正文进行规则分类。
     *
     * @param title 新闻标题
     * @param summary 新闻摘要
     * @param content 新闻正文
     * @return 分类结果
     */
    public ClassificationResult classify(String title, String summary, String content) {
        String normalizedTitle = normalize(title);
        String normalizedSummary = normalize(summary);
        String normalizedContent = normalize(content);
        String combinedText = normalizedTitle + " " + normalizedSummary + " " + normalizedContent;

        List<NewsCategoryRule> rules = loadRules();
        ClassificationResult bestResult = new ClassificationResult(
                guardianProperties.getDefaultCategoryCode(),
                guardianProperties.getDefaultCategoryName(),
                ""
        );
        int bestScore = Integer.MIN_VALUE;

        for (NewsCategoryRule rule : rules) {
            List<String> includeKeywords = splitKeywords(rule.getIncludeKeywords());
            List<String> excludeKeywords = splitKeywords(rule.getExcludeKeywords());
            Set<String> matchedKeywords = new LinkedHashSet<>();
            int score = 0;

            // 标题通常最能代表新闻主题，所以在标题中命中的关键词给予更高权重。
            for (String keyword : includeKeywords) {
                if (combinedText.contains(keyword)) {
                    matchedKeywords.add(keyword);
                    score += 1;
                    if (normalizedSummary.contains(keyword)) {
                        score += 1;
                    }
                    if (normalizedTitle.contains(keyword)) {
                        score += 2;
                    }
                }
            }

            // 排除词用于过滤“表面相关但实际不属于该类”的新闻，命中后直接大幅降权。
            for (String keyword : excludeKeywords) {
                if (combinedText.contains(keyword)) {
                    score -= 1000;
                }
            }

            if (score > bestScore) {
                bestScore = score;
                bestResult = new ClassificationResult(
                        rule.getCategoryCode(),
                        rule.getCategoryName(),
                        String.join(",", matchedKeywords)
                );
            }
        }

        return bestScore > 0 ? bestResult : new ClassificationResult(
                guardianProperties.getDefaultCategoryCode(),
                guardianProperties.getDefaultCategoryName(),
                ""
        );
    }

    /**
     * 加载启用中的分类规则。
     *
     * @return 分类规则列表
     */
    private List<NewsCategoryRule> loadRules() {
        List<NewsCategoryRule> rules = newsCategoryRuleMapper.selectList(
                new LambdaQueryWrapper<NewsCategoryRule>()
                        .eq(NewsCategoryRule::getEnabled, 1)
                        .orderByAsc(NewsCategoryRule::getPriority)
                        .orderByAsc(NewsCategoryRule::getId)
        );
        return rules == null || rules.isEmpty() ? defaultRules() : rules;
    }

    /**
     * 构造默认分类规则。
     *
     * @return 默认规则列表
     */
    private List<NewsCategoryRule> defaultRules() {
        List<NewsCategoryRule> rules = new ArrayList<>();
        rules.add(rule("MODEL", "大模型", "openai,gpt,anthropic,claude,gemini,llama,large language model,llm,foundation model", "", 10));
        rules.add(rule("RESEARCH", "AI研究", "research,paper,benchmark,multimodal,reasoning,agentic,training,alignment", "", 20));
        rules.add(rule("CHIP", "算力芯片", "nvidia,gpu,semiconductor,chip,cuda,datacenter,inference chip", "", 30));
        rules.add(rule("PRODUCT", "AI产品", "copilot,assistant,chatbot,ai tool,product launch,release", "", 40));
        rules.add(rule("POLICY", "政策监管", "regulation,regulator,law,privacy,copyright,safety,governance", "", 50));
        rules.add(rule("STARTUP", "投融资", "startup,funding,investment,venture capital,acquisition,merger", "", 60));
        rules.add(rule("GENERAL_TECH", "科技综合", "technology,software,cloud,developer,platform,app", "", 90));
        return rules;
    }

    /**
     * 创建一条默认规则。
     *
     * @param code 分类编码
     * @param name 分类名称
     * @param includeKeywords 命中关键词
     * @param excludeKeywords 排除关键词
     * @param priority 优先级
     * @return 分类规则
     */
    private NewsCategoryRule rule(String code, String name, String includeKeywords, String excludeKeywords, int priority) {
        NewsCategoryRule rule = new NewsCategoryRule();
        rule.setCategoryCode(code);
        rule.setCategoryName(name);
        rule.setIncludeKeywords(includeKeywords);
        rule.setExcludeKeywords(excludeKeywords);
        rule.setPriority(priority);
        rule.setEnabled(1);
        return rule;
    }

    /**
     * 将关键词字符串拆分为列表。
     *
     * @param keywords 关键词字符串
     * @return 关键词列表
     */
    private List<String> splitKeywords(String keywords) {
        if (!StringUtils.hasText(keywords)) {
            return new ArrayList<>();
        }
        return Arrays.stream(keywords.split("[,，\\n\\r]+"))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(StringUtils::hasText)
                .toList();
    }

    /**
     * 统一文本格式，便于匹配。
     *
     * @param text 原始文本
     * @return 归一化后的文本
     */
    private String normalize(String text) {
        return text == null ? "" : text.toLowerCase(Locale.ROOT);
    }

    /**
     * 分类结果对象。
     */
    @Data
    @AllArgsConstructor
    public static class ClassificationResult {
        private String categoryCode;
        private String categoryName;
        private String matchedKeywords;
    }
}
