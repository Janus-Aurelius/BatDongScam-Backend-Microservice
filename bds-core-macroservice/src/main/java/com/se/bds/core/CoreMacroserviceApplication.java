package com.se.bds.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class CoreMacroserviceApplication {

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(CoreMacroserviceApplication.class, args);
    }

    private static void loadDotEnv() {
        Path[] paths = {
            Paths.get(".env"),
            Paths.get("bds-core-macroservice/.env"),
            Paths.get("../.env")
        };
        for (Path path : paths) {
            if (Files.exists(path)) {
                try {
                    List<String> lines = Files.readAllLines(path);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = line.indexOf('=');
                        if (eqIdx > 0) {
                            String key = line.substring(0, eqIdx).trim();
                            String value = line.substring(eqIdx + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            } else if (value.startsWith("'") && value.endsWith("'")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            System.setProperty(key, value);
                        }
                    }
                    System.out.println("Successfully loaded environment variables from: " + path.toAbsolutePath());
                    break;
                } catch (IOException e) {
                    System.err.println("Failed to read .env file at " + path.toAbsolutePath() + ": " + e.getMessage());
                }
            }
        }
    }
}