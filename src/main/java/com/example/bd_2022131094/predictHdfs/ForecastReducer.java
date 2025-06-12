package com.example.bd_2022131094.predictHdfs;

import javafx.scene.text.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

// Reducer类：汇总结果并按序号输出
public class ForecastReducer extends Reducer<Text, Text, Text, Text> {
    private int lineNumber = 1;

    @Override
    protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
        for (Text value : values) {
            // 按序号输出结果
            String valueOf = String.valueOf(lineNumber++);
            context.write(new Text(valueOf), value);
        }
    }
}