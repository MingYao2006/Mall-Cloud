package com.mall.product.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    //    读取 yml 配置并创建 OSSClient
    @Bean
    public OSS ossClient() {
        ClientBuilderConfiguration conf = new ClientBuilderConfiguration();
        // 外网 DNS/连接偶尔抖动，收紧单次超时并限制重试次数，
        // 避免一次上传在 UnknownHost 重试上卡 20~30 秒后被前端判定超时。
        conf.setConnectionTimeout(5000);          // 建立连接 5s
        conf.setSocketTimeout(15000);             // 读取响应 15s
        conf.setMaxErrorRetry(1);                 // 失败最多重试 1 次（默认 3 次）
        conf.setConnectionRequestTimeout(3000);   // 从连接池取连接 3s
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret, conf);
    }
}
