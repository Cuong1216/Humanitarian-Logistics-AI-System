package com.humanitarian.logistics.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Humanitarian Logistics Core API.
 * The @SpringBootApplication annotation enables auto-configuration, component scanning, and property support.
 */
@SpringBootApplication
public class CoreApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApiApplication.class, args);
    }
}
