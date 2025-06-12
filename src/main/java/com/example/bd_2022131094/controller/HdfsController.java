package com.example.bd_2022131094.controller;


import com.example.bd_2022131094.config.HdfsConnection;
import com.example.bd_2022131094.domain.AjaxResult;
import com.example.bd_2022131094.util.AccuracyEvaluationUtil;
import com.example.bd_2022131094.util.CreateForecast;
import com.example.bd_2022131094.util.CreateModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import static com.example.bd_2022131094.util.HdfsUtil.*;

@RestController
@RequestMapping("/hdfs")
public class HdfsController {

    @Autowired
    private HdfsConnection hdfsConnection;
    @Autowired
    private CreateModel createModel; // 注入异步执行器
    @Autowired
    private CreateForecast createForecast;
    // 定义上传文件保存的目录（项目根目录下的upload文件夹）
    private static final String UPLOAD_DIR = "D:/hadoop/upload/";
    private static final String DOWNLOAD_DIR = "D:/hadoop/download/";

    /**
     * 创建hdfs目录
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    @GetMapping("/mkdirFilePath")
    public AjaxResult mkdirFilePath(String filePath) throws IOException {
        mkdirFileDir(hdfsConnection.getFileSystem(), filePath);
        return AjaxResult.success();
    }

    /**
     * 训练模型
     */
    @PostMapping("/trainModel")
    public AjaxResult trainModel(String inputFilePath, String outPutFilePath) throws IOException, InterruptedException, ClassNotFoundException {
//        hdfsTrainModel(hdfsConnection.getConfiguration(), inputFilePath, outPutFilePath);
        createModel.hdfsTrainModel(hdfsConnection.getConfiguration(), inputFilePath, outPutFilePath);
        return AjaxResult.success("模型正在训练");
    }

    /**
     * 预测模型
     */
    @PostMapping("/predictiveModels")
    public AjaxResult predictiveModels(String modelPath, String inputFilePath, String outputFilePath) throws Exception {
        createForecast.createForecast(hdfsConnection.getConfiguration(), inputFilePath, outputFilePath, modelPath);
        return AjaxResult.success("模型预测成功");
    }

    /**
     * 正确率评估
     */
    @PostMapping("/accuracyEvaluation")
    public AjaxResult accuracyEvaluation(String inputFilePath, String outputFilePath) throws Exception {
        double accuracyEvaluation = AccuracyEvaluationUtil.accuracyEvaluation(hdfsConnection.getFileSystem(), inputFilePath, outputFilePath);
        return AjaxResult.success(accuracyEvaluation);
    }

    /**
     * 上传文件
     *
     * @param file
     * @param filePath
     * @return
     */
    @PostMapping("/upload")
    public AjaxResult handleFileUpload(@RequestParam("file") MultipartFile file, String filePath) {
        if (file.isEmpty()) {
            return AjaxResult.error("Please select a file to upload.");
        }
        try {
            File uploadedFile = new File(UPLOAD_DIR + file.getOriginalFilename());
            //如果uploadedFile的父目录不存在，就创建
            if (!uploadedFile.getParentFile().exists()) {
                uploadedFile.getParentFile().mkdirs();
            }
            file.transferTo(uploadedFile);
            uploadFileToHDFS(hdfsConnection.getFileSystem(), uploadedFile.getAbsolutePath(), filePath + uploadedFile.getName());
            return AjaxResult.success("File uploaded successfully!");
        } catch (IOException e) {
            e.printStackTrace();
            return AjaxResult.error("File upload failed!");
        }
    }

    /**
     * 下载文件
     *
     * @param filePath
     * @param response
     * @throws IOException
     */
    @GetMapping("/download")
    public void download(String filePath, HttpServletResponse response) throws IOException {
        //获取文件名+后缀
        String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
        downFileToLocal(hdfsConnection.getFileSystem(), filePath, DOWNLOAD_DIR + fileName);
        // 获得待下载文件所在文件夹的绝对路径
        String realPath = DOWNLOAD_DIR + fileName;
        // 获得文件输入流
        FileInputStream inputStream = new FileInputStream(realPath);
        // 设置响应头、以附件形式打开文件
        // 5. 设置响应头（处理中文文件名）
        String encodedFileName = URLEncoder.encode(fileName, "UTF-8")
                .replaceAll("\\+", "%20"); // 替换空格编码
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" + encodedFileName + "\"; filename*=utf-8''" + encodedFileName);

        ServletOutputStream outputStream = response.getOutputStream();
        int len = 0;
        byte[] data = new byte[1024];
        while ((len = inputStream.read(data)) != -1) {
            outputStream.write(data, 0, len);
        }
        outputStream.close();
        inputStream.close();
    }
}
