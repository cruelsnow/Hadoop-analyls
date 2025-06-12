package com.example.bd_2022131094.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

@Slf4j
@Component
public class HdfsConnection {

    FileSystem hdfs = null;
    Configuration conf=null;

    /**
     * 初始化
     * @throws IOException
     */
    @PostConstruct
    public void init() throws IOException {
        log.info("初始化HDFS FileSystem...");
        this.conf = new Configuration();
        this.conf.set("fs.defaultFS", "hdfs://hadoop01:8020");
        System.setProperty("HADOOP_USER_NAME", "root");
        this.hdfs = FileSystem.get(this.conf);
    }

    @PreDestroy
    public void close() throws IOException {
        log.info("关闭HDFS FileSystem...");
        if (hdfs != null) {
            hdfs.close();
        }
    }

    public FileSystem getFileSystem() {
        return hdfs;
    }
    public Configuration getConfiguration() {
        return conf;
    }
}