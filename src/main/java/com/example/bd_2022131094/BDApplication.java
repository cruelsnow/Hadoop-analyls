package com.example.bd_2022131094;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // 启用异步任务支持
public class BDApplication {

    public static void main(String[] args) {
        SpringApplication.run(BDApplication.class, args);
    }

}
