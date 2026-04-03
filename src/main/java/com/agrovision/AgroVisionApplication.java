package com.agrovision;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.agrovision.entity")
@EnableJpaRepositories(basePackages = "com.agrovision.repository")
public class AgroVisionApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgroVisionApplication.class, args);
    }
}
