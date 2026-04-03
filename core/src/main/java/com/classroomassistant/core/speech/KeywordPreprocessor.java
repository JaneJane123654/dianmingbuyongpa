package com.classroomassistant.core.speech;

import java.text.Normalizer;
import java.util.*;
import java.util.locale.Locale;
import java.util.regex.Pattern;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 唤醒词预处理器
 * 
 * <p>负责将用户配置的中文唤醒词转换为拼音 tokens，与 sherpa-onnx KWS 模型词表兼容。
 * 例如："张三" → "zhang san" → "z h a ng s a n"
 * 
 * @author Code Assistant
 * @date 2026-03-24
 */
public class KeywordPreprocessor {
    private static final Logger logger = LoggerFactory.getLogger(KeywordPreprocessor.class);
    
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4E00-\\u9FFF]");
    private static final Pattern ALLOWED_CHARS = Pattern.compile("[^a-z0-9\\s]");
    
    /**
     * 预处理唤醒词，返回适配 sherpa-onnx KWS 模型的拼音 token 字符串
     * 
     * @param keyword 原始关键词（可包含中文、英文、数字）
     * @return 处理后的拼音 token 字符串，例如 "z h a ng s a n"
     */
    public static String preprocessKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return "";
        }
        
        try {
            // 1. 转拼音
            String pinyin = convertChineseToPinyin(keyword);
            if (pinyin.isEmpty()) {
                logger.warn("唤醒词 '{}' 转拼音失败，使用原值", keyword);
                return keyword.toLowerCase(Locale.ROOT);
            }
            
            // 2. 生成 ppinyin tokens（将拼音分解为单个字母）
            String ppinyinTokens = generatePinyinTokens(pinyin);
            logger.debug("关键词预处理: {} → {} → {}", keyword, pinyin, ppinyinTokens);
            return ppinyinTokens;
        } catch (Exception e) {
            logger.error("关键词预处理异常: keyword={}", keyword, e);
            return keyword.toLowerCase(Locale.ROOT);
        }
    }
    
    /**
     * 将中文转为拼音，保留非中文字符
     * 
     * @param text 输入文本
     * @return 拼音字符串
     */
    private static String convertChineseToPinyin(String text) {
        StringBuilder result = new StringBuilder();
        char[] chars = text.toCharArray();
        
        for (char c : chars) {
            // 检查是否是中文字符
            if (CHINESE_PATTERN.matcher(String.valueOf(c)).find()) {
                try {
                    String[] pinyins = PinyinHelper.toHanyuPinyinStringArray(
                        c, 
                        createPinyinFormat()
                    );
                    if (pinyins != null && pinyins.length > 0) {
                        result.append(pinyins[0]).append(" ");
                    }
                } catch (BadHanyuPinyinOutputFormatCombination e) {
                    logger.error("拼音转换异常: char={}", c, e);
                }
            } else if (Character.isLetterOrDigit(c)) {
                // 保留字母和数字
                result.append(Character.toLowerCase(c));
            } else if (Character.isWhitespace(c)) {
                // 保留空格
                result.append(" ");
            }
            // 其他字符忽略
        }
        
        // 归一化：移除重音符号、多余空格
        String normalized = Normalizer.normalize(result.toString(), Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{M}+", "");  // 移除重音标记
        normalized = normalized.replaceAll("\\s+", " ").trim();
        
        return normalized.toLowerCase(Locale.ROOT);
    }
    
    /**
     * 从拼音生成 ppinyin tokens（将拼音拆分为单个字母）
     * 例如："zhang san" → "z h a ng s a n"
     * 
     * @param pinyin 拼音字符串
     * @return ppinyin tokens
     */
    private static String generatePinyinTokens(String pinyin) {
        // 清理非法字符（仅保留 a-z 和空格）
        String cleaned = pinyin.replaceAll(ALLOWED_CHARS.pattern(), "");
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        // 按空格分割拼音音节
        String[] syllables = cleaned.split("\\s+");
        
        List<String> tokens = new ArrayList<>();
        for (String syllable : syllables) {
            if (syllable.isEmpty()) continue;
            
            // 将每个音节的字母拆分开
            for (char c : syllable.toCharArray()) {
                tokens.add(String.valueOf(c));
            }
        }
        
        return String.join(" ", tokens);
    }
    
    /**
     * 创建拼音格式配置
     */
    private static HanyuPinyinOutputFormat createPinyinFormat() {
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);  // 不带声调
        return format;
    }
    
    /**
     * 处理多个关键词（用逗号或空格分隔）
     * 
     * @param keywords 关键词字符串，多个用逗号或分号分隔
     * @return 处理后的 token 字符串列表
     */
    public static List<String> preprocessKeywords(String keywords) {
        List<String> result = new ArrayList<>();
        
        if (keywords == null || keywords.trim().isEmpty()) {
            return result;
        }
        
        // 支持逗号或分号分隔
        String[] parts = keywords.split("[,;\\s]+");
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String processed = preprocessKeyword(trimmed);
                if (!processed.isEmpty()) {
                    result.add(processed);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 合并预处理的关键词为单个传给 KWS 引擎的字符串
     * 
     * @param keywords 预处理后的 token 列表
     * @return 可直接传给 KWS 的字符串，多个关键词用逗号分隔
     */
    public static String mergeProcessedKeywords(List<String> keywords) {
        return String.join(",", keywords);
    }
}
