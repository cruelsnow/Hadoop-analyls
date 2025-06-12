package com.example.bd_2022131094.util;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.hdfs.protocol.DatanodeInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HdfsUtil {


    /**
     * 上传文件到HDFS
     *
     * @param hdfs       HDFS文件系统对象
     * @param sourceFile 本地源文件路径
     * @param targetFile HDFS目标文件路径
     * @throws IOException
     */
    public static void uploadFileToHDFS(FileSystem hdfs, String sourceFile, String targetFile) throws IOException {
        // 待上传的文件路径(windows)
        Path src = new Path(sourceFile);
        // 上传之后存放的路径(HDFS)
        Path dst = new Path(targetFile);
        // 上传
        hdfs.copyFromLocalFile(src, dst);
    }

    /**
     * 下载文件
     *
     * @param hdfs       HDFS文件系统对象
     * @param sourceFile 待下载的路径(HDFS)
     * @param targetFile 下载成功之后存放的路径(windows)
     * @throws IOException
     */
    public static void downFileToLocal(FileSystem hdfs, String sourceFile, String targetFile) throws IOException {
        // 待下载的路径(HDFS)
        Path src = new Path(sourceFile);
        // 下载成功之后存放的路径(windows)
        Path dst = new Path(targetFile);
        // 下载
        hdfs.copyToLocalFile(false, src, dst, true);
    }

    /**
     * 创建文件夹
     *
     * @param hdfs     文件系统
     * @param filePath 文件路径
     * @throws IOException
     */
    public static boolean mkdirFileDir(FileSystem hdfs, String filePath) throws IOException {
        // 待创建目录路径
        Path src = new Path(filePath);
        // 创建目录
        return hdfs.mkdirs(src);
    }

    /**
     * 创建HDFS文件
     */
    public static void createFile(FileSystem hdfs, String filePath, byte[] data) throws IOException {
        Path src = new Path(filePath);
        try (FSDataOutputStream outputStream = hdfs.create(src)) {
            outputStream.write(data);
        }
    }


    /**
     * 重命名文件
     *
     * @param hdfs
     * @param oldName 旧文件名
     * @param newName 新文件名
     * @throws IOException
     */
    public static boolean renameFile(FileSystem hdfs, String oldName, String newName) throws IOException {
        // 重命名之前的名字
        Path src = new Path(oldName);
        // 重命名之后的名字
        Path dst = new Path(newName);
        // 重命名
        return hdfs.rename(src, dst);
    }

    /**
     * 删除文件或者文件夹
     *
     * @param hdfs
     * @param path  路径
     * @param isDir 是否是文件夹true：文件，false：文件夹
     * @return
     * @throws IOException
     */
    public static boolean deleteFile(FileSystem hdfs, String path, boolean isDir) throws IOException {
        // 待删除目录路径(HDFS)
        Path src = new Path(path);
        // 删除
        if (isDir) {
            return hdfs.delete(src, false);
        }
        if (hdfs.exists(src)) {
            //递归删除
            return hdfs.delete(src, true);
        }
        return false;
    }
    /**
     * 判断文件是否存在
     */
    public static boolean exists(FileSystem hdfs, String filePath) throws IOException {
        Path path = new Path(filePath);
        return hdfs.exists(path);
    }

    /**
     * 读取HDFS某个目录下的所有文件
     */
    public static List<Map<String, Object>> readFileList(FileSystem hdfs, String filePath) throws IOException {
        Path path = new Path(filePath);
        List<Map<String, Object>> list = new ArrayList<>();
        FileStatus[] fileStatus = hdfs.listStatus(path);
        for (FileStatus file : fileStatus) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", file.getPath().getName());
            map.put("size", file.getLen());
            map.put("time", file.getModificationTime());
            map.put("permission", file.getPermission());
            map.put("owner", file.getOwner());
            map.put("group", file.getGroup());
            // 判断是目录还是文件， 是目录则返回true， 是文件则返回false
            map.put("type", file.isDirectory());
            list.add(map);
        }
        return list;
    }

    /**
     * 获取HDFS集群上所有节点名称信息
     */
    public static List<String> listDataNodeInfo(FileSystem hdfs) throws IOException {
        DistributedFileSystem fs = (DistributedFileSystem) hdfs;
        DatanodeInfo[] dataNodeStats = fs.getDataNodeStats();
        List<String> nodeNames = new ArrayList<>();
        for (DatanodeInfo datanodeInfo : dataNodeStats) {
            nodeNames.add(datanodeInfo.getHostName());
        }
        return nodeNames;
    }
}
