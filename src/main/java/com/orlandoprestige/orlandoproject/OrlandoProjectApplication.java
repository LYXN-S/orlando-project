package com.orlandoprestige.orlandoproject;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrlandoProjectApplication {

	public static void main(String[] args) {
		// Load .env file if it exists
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();
		
		// Set environment variables from .env
		dotenv.entries().forEach(entry -> 
			System.setProperty(entry.getKey(), entry.getValue())
		);
		
		SpringApplication.run(OrlandoProjectApplication.class, args);
	}

}
