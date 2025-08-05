package org.open4goods.b2bapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.open4goods")
public class NudgerB2bApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(NudgerB2bApiApplication.class, args);
    }
}
