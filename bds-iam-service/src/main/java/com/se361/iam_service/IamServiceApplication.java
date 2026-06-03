package com.se361.iam_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import com.se.bds.security.BdsSecurityAutoConfiguration;
import java.util.TimeZone;

@SpringBootApplication(exclude = { BdsSecurityAutoConfiguration.class })
@EnableFeignClients
public class IamServiceApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        SpringApplication.run(IamServiceApplication.class, args);
    }
}