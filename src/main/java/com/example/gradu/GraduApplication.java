package com.example.gradu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class GraduApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraduApplication.class, args);
    }

}
