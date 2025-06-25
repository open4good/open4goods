package org.open4goods.nudgerfrontapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "org.open4goods")
@EnableCaching
public class NudgerFrontApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NudgerFrontApiApplication.class, args);
    }
}
