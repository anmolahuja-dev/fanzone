package com.fanzone.matchthread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.fanzone.matchthread", "com.fanzone.common"})
public class MatchThreadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatchThreadServiceApplication.class, args);
    }

}
