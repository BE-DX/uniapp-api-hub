package com.uniapp.apihub;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.uniapp.apihub.module.**.mapper")
public class ApiHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiHubApplication.class, args);
    }
}
