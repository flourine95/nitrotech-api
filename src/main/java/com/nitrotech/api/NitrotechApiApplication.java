package com.nitrotech.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
public class NitrotechApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(NitrotechApiApplication.class, args);
	}

}
