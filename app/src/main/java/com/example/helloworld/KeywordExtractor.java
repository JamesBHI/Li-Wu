package com.example.helloworld;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关键词提取器
 * 从用户问题中提取核心关键词用于搜索，过滤掉无意义的停用词
 */
public class KeywordExtractor {

    // 中文停用词
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "的", "了", "是", "在", "有", "和", "就", "不", "人", "都", "一",
            "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会",
            "着", "没有", "看", "好", "自己", "这", "他", "她", "它", "们",
            "我", "我们", "你们", "他们", "它们", "什么", "怎么", "为什么",
            "如何", "哪", "哪些", "多少", "请问", "可以", "能", "帮", "一下",
            "吗", "请", "告诉", "知道", "有没有", "是不是", "能不能", "会不会",
            "要不要", "行不行", "可不可以", "为什么", "怎样", "怎么", "咋",
            "啥", "谁", "何时", "哪里", "何处", "哪个", "为何", "若何",
            "的话", "的时候", "之", "与", "及", "或", "但", "而", "且",
            "因为", "所以", "虽然", "如果", "然后", "之后", "以前", "时",
            "把", "被", "让", "给", "对", "对于", "关于", "根据", "按照",
            "从", "到", "在", "向", "往", "于", "跟", "和", "与", "同",
            "这", "那", "哪", "某", "每", "各", "该", "此",
            "啊", "吧", "呢", "吗", "呀", "嘛", "噢", "哦", "嗯", "哈",
            "来", "去", "做", "搞", "弄", "让", "使",
            "非常", "很", "太", "极", "更", "最", "比较", "相当",
            "已经", "曾经", "正在", "将要", "刚刚", "才", "就",
            "还", "也", "再", "又", "都", "只", "仅", "不过",
            "其实", "当然", "确实", "的确", "真的",
            "这个", "那个", "这些", "那些", "这里", "那里",
            "一般", "通常", "往往", "常常", "经常", "有时",
            "可能", "也许", "大概", "大约", "似乎", "好像",
            "第一", "第二", "首先", "其次", "然后", "最后",
            "例如", "比如", "譬如", "包括", "等等",
            "通过", "利用", "使用", "采用", "借助",
            "所以", "因此", "因而", "从而", "于是"
    ));

    // 专业术语后缀（保留这些词结尾的词组）
    private static final Set<String> TECH_SUFFIXES = new HashSet<>(Arrays.asList(
            "定律", "定理", "公式", "原理", "效应", "现象", "方程", "函数",
            "速度", "加速度", "力", "质量", "能量", "动量", "冲量", "功",
            "功率", "频率", "波长", "振幅", "周期", "电压", "电流", "电阻",
            "电容", "电感", "磁场", "电场", "引力", "摩擦力", "弹力",
            "压力", "压强", "体积", "密度", "温度", "热量", "熵",
            "位移", "路程", "时间", "时刻", "角度", "弧度", "面积",
            "竞赛", "考试", "题目", "题型", "解法", "思路", "技巧"
    ));

    /**
     * 从用户问题中提取关键词
     * @param query 用户原始问题
     * @return 提取后的关键词字符串，用于搜索引擎
     */
    public static String extractKeywords(String query) {
        // 1. 先用引号内的内容（如果有）
        Pattern quotePattern = Pattern.compile("[\"\"]([^\"\"]+)[\"\"]");
        Matcher quoteMatcher = quotePattern.matcher(query);
        StringBuilder quotedTerms = new StringBuilder();
        String remaining = query;
        while (quoteMatcher.find()) {
            quotedTerms.append(quoteMatcher.group(1)).append(" ");
        }
        remaining = quotePattern.matcher(remaining).replaceAll("");

        // 2. 分词（中文按字符+词组，英文按空格）
        List<String> tokens = tokenize(remaining);

        // 3. 过滤停用词 + 保留专业术语
        List<String> keywords = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            // 数字保留
            if (trimmed.matches("[0-9\\\\.\\-]+")) {
                keywords.add(trimmed);
                continue;
            }
            // 英文单词保留（长度>=2）
            if (trimmed.matches("[a-zA-Z]{2,}")) {
                keywords.add(trimmed);
                continue;
            }
            // 中文词：过滤停用词
            if (trimmed.length() >= 2 && !STOP_WORDS.contains(trimmed)) {
                keywords.add(trimmed);
                continue;
            }
            // 单字中文：如果是专业术语后缀的一部分，保留
            if (trimmed.length() == 1 && isChinese(trimmed)) {
                // 单字中文词基本是停用词，跳过
                continue;
            }
        }

        // 4. 构建搜索关键词（取前6个）
        StringBuilder result = new StringBuilder();
        result.append(quotedTerms.toString().trim());
        int count = 0;
        for (String kw : keywords) {
            if (count >= 6) break;
            if (result.length() > 0) result.append(" ");
            result.append(kw);
            count++;
        }

        String finalQuery = result.toString().trim();
        if (finalQuery.isEmpty()) {
            // 如果提取后为空，返回原查询的前20个字
            return query.length() > 30 ? query.substring(0, 30) : query;
        }
        return finalQuery;
    }

    /**
     * 简单分词
     */
    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c) || c == '.' || c == '-' || c == '+') {
                buffer.append(c);
            } else {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString());
                    buffer.setLength(0);
                }
            }
        }
        if (buffer.length() > 0) {
            tokens.add(buffer.toString());
        }

        // 额外的中文双字词提取（简单相邻双字组合）
        // 例如 "物理竞赛" 应作为一个词
        // 对于中文文本，尝试提取2-gram
        String chineseOnly = text.replaceAll("[^\\u4e00-\\u9fff]", " ");
        String[] parts = chineseOnly.split("\\s+");
        for (String part : parts) {
            if (part.length() >= 4) { // 长度>=2个汉字
                for (int i = 0; i < part.length() - 1; i++) {
                    String bigram = part.substring(i, i + 2);
                    // 检查是否是常见的专业组合
                    if (!STOP_WORDS.contains(bigram)) {
                        tokens.add(bigram);
                    }
                }
            }
        }

        return tokens;
    }

    private static boolean isChinese(String s) {
        return s.matches("[\\u4e00-\\u9fff]+");
    }
}
