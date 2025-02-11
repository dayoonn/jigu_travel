package com.project_merge.jigu_travel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients  // FeignClient 활성화
@EnableScheduling
public class JiguTravelApplication {

	public static void main(String[] args) {

		SpringApplication.run(JiguTravelApplication.class, args);

	}

}
