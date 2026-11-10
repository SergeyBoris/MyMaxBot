package org.example.spring;
import org.example.spring.config.BotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "org.example.spring",
        "org.example.services",
        "org.example.spring.repo",
        "org.example.db",
        "org.example.entity",
        "org.example.util",
        "org.example.processors"
})
@EnableScheduling
@EnableConfigurationProperties(BotProperties.class)
@EntityScan(basePackages = "org.example.entity")
@EnableJpaRepositories(basePackages = "org.example.spring.repo")
public class BotApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");
        SpringApplication.run(BotApplication.class, args);
    }
}
