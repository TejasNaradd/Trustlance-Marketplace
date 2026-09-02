package com.trustlance.escrowservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EscrowServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EscrowServiceApplication.class, args);
    }
}
