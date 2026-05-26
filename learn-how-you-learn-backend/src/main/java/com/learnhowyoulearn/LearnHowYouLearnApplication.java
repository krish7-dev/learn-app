package com.learnhowyoulearn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableJpaAuditing
@EnableAsync
public class LearnHowYouLearnApplication {

	public static void main(String[] args) {
		SpringApplication.run(LearnHowYouLearnApplication.class, args);
	}

}
