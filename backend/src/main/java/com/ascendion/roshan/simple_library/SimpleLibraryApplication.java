package com.ascendion.roshan.simple_library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SimpleLibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleLibraryApplication.class, args);
	}

}
