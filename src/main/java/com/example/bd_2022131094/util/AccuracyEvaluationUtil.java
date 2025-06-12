package com.example.bd_2022131094.util;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccuracyEvaluationUtil {


    /**
     * 评估准确率
     *
     * @param fs                    HDFS文件系统
     * @param predictTheOutcomePath 预测结果文件路径
     * @param filePath              要评估文件路径
     * @return
     * @throws Exception
     */
    public static double accuracyEvaluation(FileSystem fs, String predictTheOutcomePath, String filePath) throws Exception {

        // 路径
        Path predictedPath = new Path(predictTheOutcomePath);

        Path testPath = new Path(filePath);

        // 读取预测结果
        Map<Integer, String> predictedLabels = new HashMap<>();
        BufferedReader predReader = new BufferedReader(new InputStreamReader(fs.open(predictedPath)));
        String line;
        while ((line = predReader.readLine()) != null) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length >= 2) {
                int id = Integer.parseInt(parts[0]);
                String label = parts[1].trim().toLowerCase(); // 用小写处理
                predictedLabels.put(id, label);
            }
        }
        predReader.close();

        // 读取测试文件
        Map<Integer, List<String>> trueLabels = new HashMap<>();
        BufferedReader testReader = new BufferedReader(new InputStreamReader(fs.open(testPath)));
        int index = 1;
        while ((line = testReader.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length > 0) {
                String genres = parts[0];
                genres = genres.replaceAll("[\\[\\]' ]", ""); // 去除多余字符
                String[] labels = genres.split(",");
                List<String> labelList = new ArrayList<>();
                for (String l : labels) {
                    if (!l.isEmpty()) {
                        labelList.add(l.trim().toLowerCase());
                    }
                }
                trueLabels.put(index, labelList);
            }
            index++;
        }
        testReader.close();

        // 比对
        int total = 0;
        int correct = 0;

        for (Map.Entry<Integer, String> entry : predictedLabels.entrySet()) {
            int id = entry.getKey();
            String predicted = entry.getValue();

            List<String> trueList = trueLabels.get(id);
            if (trueList != null) {
                total++;
                if (trueList.contains(predicted)) {
                    correct++;
                }
            }
        }

        // 输出准确率
        double accuracy = total == 0 ? 0 : (double) correct / total;
        System.out.printf("预测总数：%d，预测正确数：%d，准确率：%.2f%%\n", total, correct, accuracy * 100);
        return accuracy;
    }
}
