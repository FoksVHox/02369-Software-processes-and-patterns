package com.group6.SongQueue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SongQueueApplication {

	public static void main(String[] args) {
		SpringApplication.run(SongQueueApplication.class, args);
	}

}
