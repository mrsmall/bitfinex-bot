package com.klein.btc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.ApiContextInitializer;

@SpringBootApplication
public class BtcApplication {

	@Bean
	public ArbitrageBot arbitrageBot(){
		return new ArbitrageBot();
	}

	public static void main(String[] args) {
		ApiContextInitializer.init();
		SpringApplication.run(BtcApplication.class, args);
	}
}
