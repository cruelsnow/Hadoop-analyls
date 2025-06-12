package com.example.bd_2022131094.util;
import com.example.bd_2022131094.predictHdfs.ForecastMapper;
import com.example.bd_2022131094.predictHdfs.ForecastReducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;

/**
 * 生成预测
 */
@Slf4j
@Component
public class CreateForecast {
    /**
     * 生成预测
     * @param conf
     * @param inputPathString 要预测的文件路径
     * @param outputPathString  预测结果输出路径
     * @param modelPathString 模型路径
     * @throws Exception
     */
    @Async
    public  void createForecast(Configuration conf,String inputPathString,  String outputPathString,String modelPathString) throws Exception {
//        String inputPathString = "/input_2022131094/aaa.txt";
//        String outputPathString = "/output_2022131094/2022131094_预测19.txt";
//        String modelPathString = "/output_2022131094/2022131094_模型/2022131094_模型.txt";
//        Configuration conf = new Configuration();
//        conf.set("fs.defaultFS", "hdfs://hadoop01:8020");
//        System.setProperty("HADOOP_USER_NAME", "root");
//        conf.set("yarn.resourcemanager.hostname", "hadoop02");
        conf.set("model.path", modelPathString); // 动态设置模型路径
        conf.set("input.path", inputPathString); // 动态设置模型路径
        conf.set("output.path", outputPathString); // 动态设置模型路径

        // 创建MapReduce作业
        Job job = Job.getInstance(conf, "Movie Category Forecast");
        job.setJarByClass(CreateForecast.class);
        job.setMapperClass(ForecastMapper.class);
        job.setReducerClass(ForecastReducer.class);

        // 设置输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
//        // 设置输入输出路径
        FileInputFormat.addInputPath(job, new Path(inputPathString));
        FileOutputFormat.setOutputPath(job, new Path(outputPathString+"1"));
        // Hadoop 2.x中移除配置的正确方式
        conf.unset("model.path");
        conf.unset("input.path");
        conf.unset("output.path");
        // 提交作业并等待完成
        boolean success = job.waitForCompletion(true);
    }
}