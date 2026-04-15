package com.playlist.plitter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PlitterApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlitterApplication.class, args);
    }

}
