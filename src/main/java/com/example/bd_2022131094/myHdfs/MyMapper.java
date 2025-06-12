package com.example.bd_2022131094.myHdfs;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

// 自定义Mapper类
// Mapper类：提取标签和关键词并输出<word, 1>
public class MyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {

    private final static LongWritable one = new LongWritable(1);
    private Text word = new Text();

    protected void map(LongWritable key, Text value, Context context)
            throws IOException, InterruptedException {

        Pattern pattern = Pattern.compile("^\\[(.*?)\\]\\s*\\[(.*?)\\]$");
        Matcher matcher = pattern.matcher(value.toString());

        if (matcher.find()) {
            List<String> tags = processTags(matcher.group(1));
            List<String> keywords = processKeywords(matcher.group(2));
            // 输出标签计数
            for (String tag : tags) {
                // 跳过包含中文的标签
                if (containsChinese(tag)) {
                    continue;
                }
                for (String keyword : keywords) {
                    // 跳过包含中文的标签
                    if (containsChinese(keyword)) {
                        continue;
                    }
                    word.set(tag + "_" + keyword);
                    context.write(word, one);
                }
            }
            for (String tag : tags) {
                // 跳过包含中文的标签
                if (containsChinese(tag)) {
                    continue;
                }
                word.set(tag);
                context.write(word, one);
            }
        }
    }
    // 新增：检查字符串是否包含中文字符的方法
    private boolean containsChinese(String str) {
        String chinesePattern = "[\u4e00-\u9fa5]"; // 中文字符的Unicode范围
        return str.matches(".*" + chinesePattern + ".*");
    }
    private List<String> processTags(String tagsStr) {
        return Arrays.stream(tagsStr.split(",\\s*"))
                .map(tag -> tag.replaceAll("'", "").trim())
                .map(tag -> tag.replaceAll("\"", "").trim())
                .filter(tag -> !tag.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> processKeywords(String keywordsStr) {
        return Arrays.stream(keywordsStr.split(",\\s*"))
                .map(keyword -> keyword.replaceAll("'", "").trim().toLowerCase())
                .map(keyword -> keyword.replaceAll("\"", "").trim().toLowerCase())
                .filter(keyword -> !keyword.isEmpty())
                .collect(Collectors.toList());
    }
}
