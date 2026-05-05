package com.se.bds.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

//TODO: disable the datasource when connect to database
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class CoreMacroserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreMacroserviceApplication.class, args);
    }
}