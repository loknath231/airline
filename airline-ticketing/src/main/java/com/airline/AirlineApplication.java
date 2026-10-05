package com.airline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main class to start airline-ticketing
 *
 * @author Loknath Kumar
 * @version 1.0
 * @since 2026-03-01
 */
@SpringBootApplication
@EnableScheduling
public class AirlineApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineApplication.class, args);
    }
}
