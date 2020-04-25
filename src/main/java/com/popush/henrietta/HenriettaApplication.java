package com.popush.henrietta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableCaching
@EnableAsync
@SpringBootApplication
public class HenriettaApplication {
    public static void main(String[] args) {
        SpringApplication.run(HenriettaApplication.class, args);
    }
}
