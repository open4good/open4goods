package org.open4goods.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

import de.codecentric.boot.admin.server.config.EnableAdminServer;

@SpringBootApplication
@EnableAdminServer
@EnableCaching
public class SbAdminApplication {

	public static void main(String[] args) {
		SpringApplication.run(SbAdminApplication.class, args);
	}
}
