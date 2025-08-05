package com.example.gradu;

import com.example.gradu.global.config.EmailProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
@EnableConfigurationProperties(EmailProperties.class)
public class GraduApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraduApplication.class, args);
    }

}
