package com.akulaku.transaction.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationBeansConfiguration {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
