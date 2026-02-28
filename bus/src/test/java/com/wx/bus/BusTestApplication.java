package com.wx.bus;

import com.wx.bus.config.EnableWxBus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 测试用入口：加载 bus 组件上下文，供 @SpringBootTest 使用。
 */
@SpringBootApplication(scanBasePackages = "com.wx.bus")
@EnableWxBus
public class BusTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTestApplication.class, args);
    }
}
