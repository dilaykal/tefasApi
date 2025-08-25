package com.dilaykal.staj_proje;

import jakarta.persistence.Entity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = {"com.dilaykal"})
@ComponentScan(basePackages = {"com.dilaykal"})
@EnableJpaRepositories(basePackages = {"com.dilaykal"})
@EnableScheduling
public class StajProjeApplication {

	public static void main(String[] args) {
		SpringApplication.run(StajProjeApplication.class, args);
	}

}
