package com.example.stockmonitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class StockMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockMonitorApplication.class, args);
    }
}