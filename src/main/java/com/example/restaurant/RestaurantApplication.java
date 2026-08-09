package com.example.restaurant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class RestaurantApplication {

    static void main(String[] args) {
        SpringApplication.run(RestaurantApplication.class, args);
    }

}
