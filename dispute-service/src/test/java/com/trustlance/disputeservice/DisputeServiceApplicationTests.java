package com.trustlance.disputeservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092",
    "spring.data.mongodb.uri=mongodb://localhost:27017/test_db"
})
class DisputeServiceApplicationTests {
    @Test void contextLoads() {}
}
