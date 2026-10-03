package com.uav.uavserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UAVServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(UAVServerApplication.class, args);
    }
}
