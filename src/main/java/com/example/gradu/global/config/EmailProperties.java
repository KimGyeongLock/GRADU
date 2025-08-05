package com.example.gradu.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@Getter
@ConfigurationProperties(prefix = "app.email")
@RequiredArgsConstructor(onConstructor_ = @ConstructorBinding)
public class EmailProperties {
    private final String domain;
}