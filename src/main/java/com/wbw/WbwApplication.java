package com.wbw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WbwApplication {

    public static void main(String[] args) {
        SpringApplication.run(WbwApplication.class, args);
    }
}
