package com.example.bd_2022131094.myHdfs;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MyReducer extends Reducer<Text, LongWritable, Text, LongWritable> {

    private LongWritable result = new LongWritable();
    private List<WordCount> wordList = new ArrayList<>(); // 用于存储单词及其频率

    // Reducer的setup方法：初始化时清空列表（可选）
    @Override
    protected void setup(Context context) {
        wordList.clear();
    }

    // reduce方法：收集所有单词和频率
    @Override
    public void reduce(Text key, Iterable<LongWritable> values, Context context) {
        long sum = 0;
        for (LongWritable val : values) {
            sum += val.get();
        }
        // 将单词和频率存入列表
        wordList.add(new WordCount(key.toString(), sum));
    }

    // Reducer的cleanup方法：在所有reduce任务完成后执行排序和输出
    @Override
    protected void cleanup(Context context) throws IOException, InterruptedException {
        // 1. 对单词按字母顺序排序（可自定义排序规则）
        wordList.sort(Comparator.comparing(WordCount::getWord));

        List<WordCount> tmpList = new ArrayList<>();
        // 2. 分配序号并输出
        int order = 1; // 序号从1开始
        for (WordCount wc : wordList) {
            // 构造输出格式："序号,单词,频率"
            if (wc.getWord().contains("_")) {
                String output = wc.getWord() + "  " + order + "\t" + wc.getCount();
                // 将结果写入上下文（注意：输出的key和value需符合MapReduce规范）
                context.write(new Text(output), null); // key为文本格式，value可留空或填0
                order++;
            } else {
                tmpList.add(wc);
            }
        }
        int tmpOrder = 1;
        for (WordCount wordCount : tmpList) {
            String output = wordCount.getWord() + "  " + tmpOrder + "\t" + wordCount.getCount();
            // 将结果写入上下文（注意：输出的key和value需符合MapReduce规范）
            context.write(new Text(output), null); // key为文本格式，value可留空或填0
            tmpOrder++;
        }
    }

    // 自定义内部类：存储单词和频率
    private static class WordCount {
        private String word;
        private long count;

        public WordCount(String word, long count) {
            this.word = word;
            this.count = count;
        }

        public String getWord() {
            return word;
        }

        public long getCount() {
            return count;
        }
    }
}