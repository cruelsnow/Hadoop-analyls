package com.example.bd_2022131094.util;

import com.example.bd_2022131094.myHdfs.MyMapper;
import com.example.bd_2022131094.myHdfs.MyReducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;


/**
 * 创建模型，预测
 */
@Slf4j
@Component
public class CreateModel {

    @Async
    public  void hdfsTrainModel(Configuration conf, String inputFilePath, String outPutFilePath) throws IOException, InterruptedException, ClassNotFoundException {
        Path inputPath = new Path(inputFilePath);
        Path outputPath = new Path(outPutFilePath);
        FileSystem fs = FileSystem.get(conf);
        if (fs.exists(outputPath)) {
            fs.delete(outputPath, true);
        }
        Job job = Job.getInstance(conf, "Sentiment Classifier Training");
        job.setJarByClass(CreateModel.class);
        // 设置Mapper和Reducer
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        // 设置输出类型
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        // 设置输入输出格式
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        // 设置输入输出路径
        FileInputFormat.addInputPath(job, inputPath);
        FileOutputFormat.setOutputPath(job, outputPath);
//        System.exit(job.waitForCompletion(true) ? 0 : 1);
        // 正确做法：移除 exit 调用，让异步线程自行结束
        boolean success = job.waitForCompletion(true);
        if (success) {
            // 将默认输出文件重命名为自定义名称
//            FileSystem fs = FileSystem.get(conf);
            int i = outPutFilePath.lastIndexOf("/");
            Path defaultOutput = new Path(outPutFilePath + "/part-r-00000");
            Path customOutput = new Path(outPutFilePath + "/" + outPutFilePath.substring(i + 1) + ".txt");
            if (fs.exists(defaultOutput)) {
                fs.rename(defaultOutput, customOutput);
            }
        }
    }
}
