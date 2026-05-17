package com.maciu19.jmix2springboot;

import com.google.common.base.Strings;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class Jmix2springbootApplication {

    private final Environment environment;

    public Jmix2springbootApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(Jmix2springbootApplication.class, args);
    }

    @EventListener
    public void printApplicationUrl(final ApplicationStartedEvent event) {
        LoggerFactory.getLogger(Jmix2springbootApplication.class)
                .info("Application started at http://localhost:{} {}",
                        environment.getProperty("local.server.port"),
                        Strings.nullToEmpty(environment.getProperty("server.servlet.context-path")));
    }
}
