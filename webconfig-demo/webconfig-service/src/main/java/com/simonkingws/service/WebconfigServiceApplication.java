package com.simonkingws.service;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDubbo
@EnableFeignClients("com.simonkingws")
public class WebconfigServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebconfigServiceApplication.class, args);
    }

}
