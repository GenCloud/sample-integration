package org.genfork.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * @author: GenCloud
 * @date: 2020/07
 */
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class AppInit {
	public static void main(String[] args) {
		SpringApplication.run(AppInit.class, args);
	}
}
