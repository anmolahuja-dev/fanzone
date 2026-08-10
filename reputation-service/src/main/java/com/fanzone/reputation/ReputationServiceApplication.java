package com.fanzone.reputation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.fanzone.reputation", "com.fanzone.common"})
public class ReputationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReputationServiceApplication.class, args);
    }

}
