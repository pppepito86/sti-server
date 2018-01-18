package org.pesho.judge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

@Configuration
@SpringBootApplication
@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableScheduling
public class NoiApplication {
	
	public ThymeleafViewResolver thymeleafViewResolver() {
	    ThymeleafViewResolver resolver = new ThymeleafViewResolver();
	    resolver.setCharacterEncoding("UTF-8");
	    return resolver;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(NoiApplication.class, args);
	}
	
}
