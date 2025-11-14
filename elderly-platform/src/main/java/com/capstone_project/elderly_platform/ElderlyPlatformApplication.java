package com.capstone_project.elderly_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ElderlyPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(ElderlyPlatformApplication.class, args);
	}

}
