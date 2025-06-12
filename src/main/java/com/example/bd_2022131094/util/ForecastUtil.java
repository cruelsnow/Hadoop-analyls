package com.example.bd_2022131094.util;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ForecastUtil {
    // 使用实例变量替代静态变量，确保每个实例有自己的状态
    private final ConcurrentMap<String, Integer> categoryCounts = new ConcurrentHashMap<>(); // Nc
    private final ConcurrentMap<String, ConcurrentMap<String, Integer>> wordCounts = new ConcurrentHashMap<>(); // Ncw
    private volatile int totalDocuments = 0;
    private final Set<String> vocabulary = ConcurrentHashMap.newKeySet();
    public  void createForecast(FileSystem fs, String modelPath, String inputFilePath, String outputFilePath) throws IOException {
        loadModel(fs, modelPath);
        List<String> moviesToPredict = readFile(fs, inputFilePath);
        // 3. 进行预测并输出结果
        predictAndSave(fs, moviesToPredict, outputFilePath);
    }

    // 加载模型文件（处理带序号的格式）
    private void loadModel(FileSystem fs, String modelPath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(modelPath))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 示例行格式：
                // 类别行："Family 1\t29504"
                // 词计数行："Thriller_maquiladora 1\t64"
                String[] parts = line.split("\t");
                if (parts.length != 2) continue;
                // 提取键和计数
                String fullKey = parts[0];
                int count = Integer.parseInt(parts[1]);
                // 分离键和可能的序号
                String[] keyParts = fullKey.split(" ");
                String mainKey = keyParts[0]; // 实际使用的键
                // 处理类别计数
                if (!mainKey.contains("_")) {
                    categoryCounts.put(mainKey, count);
                    // 使用原子方式更新总文档数
                    synchronized (this) {
                        totalDocuments += count;
                    }
                }
                // 处理词计数
                else {
                    String[] categoryWord = mainKey.split("_");
                    if (categoryWord.length != 2) continue;

                    String category = categoryWord[0];
                    String word = categoryWord[1];

                    vocabulary.add(word);
                    wordCounts.computeIfAbsent(category, k -> new ConcurrentHashMap<>())
                            .put(word, count);
                }
            }
        }
    }

    // 读取待预测文件
    private List<String> readFile(FileSystem fs, String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(filePath))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    // 预测并保存结果（严格符合格式要求）
    private void predictAndSave(FileSystem fs, List<String> movies, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(outputPath), true)))) {
            for (int i = 0; i < movies.size(); i++) {
                System.out.println("正在处理第" + (i + 1) + "条数据...");
                String line = movies.get(i);
                String predictedCategory = predictCategory(line);
                // 严格格式：序号从1开始，空格分隔，无其他字符
                writer.write((i + 1) + " " + predictedCategory + "\n");
            }
        }
    }

    // 预测单条记录的类别
    private String predictCategory(String movieLine) {
        // 提取关键词部分
        Matcher matcher = Pattern.compile("^.*?\\t\\[(.*?)\\]$").matcher(movieLine);
        if (!matcher.find()) return "Unknown";

        String keywordsStr = matcher.group(1);
        List<String> keywords = Arrays.asList(keywordsStr.split(",\\s*"));

        // 创建当前类别的副本，防止在计算过程中被修改
        Map<String, Integer> localCategoryCounts = new HashMap<>(categoryCounts);
        Map<String, Map<String, Integer>> localWordCounts = new HashMap<>();
        wordCounts.forEach((k, v) -> localWordCounts.put(k, new HashMap<>(v)));

        Set<String> localVocabulary = new HashSet<>(vocabulary);
        int localTotalDocuments = totalDocuments; // volatile读取，保证可见性

        // 计算每个类别的对数概率
        Map<String, Double> scores = new HashMap<>();
        for (String category : localCategoryCounts.keySet()) {
            double logProb = Math.log((double) localCategoryCounts.get(category) / localTotalDocuments);

            // 计算P(w|c)的乘积（对数形式为求和）
            for (String word : keywords) {
                word = word.replaceAll("'", "").trim().toLowerCase();
                if (!localVocabulary.contains(word)) continue;

                int wordCount = localWordCounts.getOrDefault(category, Collections.emptyMap())
                        .getOrDefault(word, 0);
                int totalWordsInCategory = localWordCounts.getOrDefault(category, Collections.emptyMap())
                        .values().stream()
                        .mapToInt(Integer::intValue).sum();

                // 使用拉普拉斯平滑
                double prob = (wordCount + 1.0) / (totalWordsInCategory + localVocabulary.size());
                logProb += Math.log(prob);
            }

            scores.put(category, logProb);
        }
        // 返回概率最高的类别
        return Collections.max(scores.entrySet(), Map.Entry.comparingByValue()).getKey();
    }
}
