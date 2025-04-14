// src/main/java/com/guvi/busapp/BusappApplication.java
package com.guvi.busapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync; // Import EnableAsync

@SpringBootApplication
@EnableAsync // Enable asynchronous method execution
public class BusappApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusappApplication.class, args);
	}

}