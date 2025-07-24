package org.open4goods.staticserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.open4goods")
public class StaticApplication {
    public static void main(String[] args) {
        SpringApplication.run(StaticApplication.class, args);
    }
}
