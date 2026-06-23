package com.se100.bds.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiGatewayApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(ApiGatewayApplication.class, args);
        } catch (Throwable t) {
            System.err.println("=== FATAL STARTUP ERROR IN API GATEWAY ===");
            t.printStackTrace(System.err);
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
                System.err.println("Caused by: " + cause);
                cause.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }
}
