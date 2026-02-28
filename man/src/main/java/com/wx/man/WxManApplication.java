package com.wx.man;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = {"com.wx.man", "com.wx.bus"})
@EnableMongoRepositories(basePackages = "com.wx.bus.infrastructure.mongo")
public class WxManApplication {

    public static void main(String[] args) {
        SpringApplication.run(WxManApplication.class, args);
    }
}
