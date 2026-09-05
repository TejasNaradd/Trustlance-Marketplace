package com.trustlance.aiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;
import com.trustlance.aiservice.config.MinioProperties;

@SpringBootApplication
@EnableDiscoveryClient
@EnableAsync
@EnableConfigurationProperties(MinioProperties.class)
public class AiServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiServiceApplication.class, args);
    }
}
