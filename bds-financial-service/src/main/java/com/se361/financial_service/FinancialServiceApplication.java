package com.se361.financial_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.se.bds.security.BdsSecurityAutoConfiguration;

@SpringBootApplication(scanBasePackages = {"com.se361.financial_service", "com.se.bds.security"})
@EnableScheduling
public class FinancialServiceApplication {

	public static void main(String[] args) {

        SpringApplication.run(FinancialServiceApplication.class, args);
	}

}
