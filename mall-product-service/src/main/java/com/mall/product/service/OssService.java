package com.mall.product.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.mall.product.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

//OSS 工具类，封装上传和删除方法
@Slf4j
@Service
public class OssService {

    private final OSS ossClient;
    private final OssConfig ossConfig;

    public OssService(OSS ossClient, OssConfig ossConfig) {
        this.ossClient = ossClient;
        this.ossConfig = ossConfig;
    }

    /**
     * 上传文件到 OSS
     *
     * @param file   文件
     * @param folder 文件夹路径，如 "product/"
     * @return 文件的公网 URL
     */
    public String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String objectName = folder + UUID.randomUUID().toString().replace("-", "") + extension;

        try (InputStream inputStream = file.getInputStream()) {
//            创建 OSS 对象元数据
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
//            执行上传操作
            ossClient.putObject(ossConfig.getBucketName(), objectName, inputStream, metadata);

            // 返回公网访问 URL
            String url = "https://" + ossConfig.getBucketName() + "." + ossConfig.getEndpoint() + "/" + objectName;
            log.info("文件上传成功: {}", url);
            return url;
        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage());
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 删除 OSS 上的文件
     *
     * @param fileUrl 文件的公网 URL
     */
    public void delete(String fileUrl) {
        try {
            String objectName = fileUrl.substring(fileUrl.indexOf(".com/") + 5);
            ossClient.deleteObject(ossConfig.getBucketName(), objectName);
            log.info("文件删除成功: {}", fileUrl);
        } catch (Exception e) {
            log.warn("文件删除失败: {}", e.getMessage());
        }
    }
}
