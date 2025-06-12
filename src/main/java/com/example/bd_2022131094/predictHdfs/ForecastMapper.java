package com.example.bd_2022131094.predictHdfs;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Mapper;

import javax.xml.soap.Text;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
// Mapper类：处理单条记录并生成预测
public class ForecastMapper extends Mapper<Object, Text, Text, Text> {

    private static final ConcurrentMap<String, Integer> categoryCounts = new ConcurrentHashMap<>(); // Nc
    private static final ConcurrentMap<String, ConcurrentMap<String, Integer>> wordCounts = new ConcurrentHashMap<>(); // Ncw
    private static volatile int totalDocuments = 0;
    private static final Set<String> vocabulary = ConcurrentHashMap.newKeySet();
    private static final int BATCH_SIZE = 1000; // 每批处理1000条数据
    private static  int currentLine  = 0; // 第几行
    @Override
    protected void setup(Context context) throws IOException {
        // 从HDFS加载模型（每个Mapper都加载一次，实际生产环境可优化为分布式缓存）
        Configuration conf = context.getConfiguration();
        String modelPath = conf.get("model.path");
        String inputPath = conf.get("input.path");
        String outputPath = conf.get("output.path");
        FileSystem fs = FileSystem.get(conf);
        // 1. 加载训练好的模型
        loadModel(fs, modelPath);
        // 2. 读取待预测数据
        List<String> moviesToPredict = readFile(fs, inputPath);
        // 3. 进行预测并输出结果
        predictAndSave(fs, moviesToPredict, outputPath);
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
// 预测并保存结果（支持断点续传）
    // 预测并保存结果（分批处理，每1000条一批）
    private void predictAndSave(FileSystem fs, List<String> movies, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(new Path(outputPath), true)))) {
            int totalRecords = movies.size();
            int batchSize = 1000;
            int totalBatches = (totalRecords + batchSize - 1) / batchSize; // 计算总批次数

            for (int batch = 0; batch < totalBatches; batch++) {
                int startIndex = batch * batchSize;
                int endIndex = Math.min(startIndex + batchSize, totalRecords);
                List<String> batchData = movies.subList(startIndex, endIndex);
                // 处理当前批次
                processBatch(writer, batchData, startIndex);
            }
        }
    }

    // 处理单个批次的数据
    private void processBatch(BufferedWriter writer, List<String> batch, int startIndex) throws IOException {
        for (int i = 0; i < batch.size(); i++) {
            String line = batch.get(i);
            String predictedCategory = predictCategory(line);
            int globalLineNumber = startIndex + i + 1; // 全局行号，从1开始
            // 每处理100条数据显示一次进度
            if ((i + 1) % 2000 == 0) {
                log.info("正在处理第 {} 条数据", globalLineNumber);
            }
            writer.write(globalLineNumber + " " + predictedCategory + "\n");
        }
        writer.flush(); // 刷新缓冲区，确保数据写入
    }
// 读取待预测文件（分批读取）
    private List<String> readFile(FileSystem fs, String filePath) throws IOException {
        List<String> batchLines = new ArrayList<>(BATCH_SIZE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(filePath))))) {
            String line;
            // 跳过已读取的行
            for (int i = 0; i < currentLine; i++) {
                if (reader.readLine() == null) break;
            }
            // 读取当前批次的1000条数据
            while ((line = reader.readLine()) != null && batchLines.size() < BATCH_SIZE) {
                batchLines.add(line);
                currentLine++;
            }
        }
        return batchLines;
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
