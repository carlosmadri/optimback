package com.airbus.optim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OptimApplication {

	public static void main(String[] args) {
		SpringApplication.run(OptimApplication.class, args);
	}

}
