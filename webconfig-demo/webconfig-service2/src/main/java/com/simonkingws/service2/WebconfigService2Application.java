package com.simonkingws.service2;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDubbo
@EnableFeignClients("com.simonkingws")
public class WebconfigService2Application {

    public static void main(String[] args) {
        SpringApplication.run(WebconfigService2Application.class, args);
    }

}
