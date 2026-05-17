package com.maciu19.jmix2springboot.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

@Configuration
@EnableJpaAuditing
public class PersistenceConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        // TODO: Change when we add Spring Security and Authentik
        return () -> Optional.of("system");
    }
}
