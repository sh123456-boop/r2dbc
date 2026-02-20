package com.example.tomcat.ws;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class WsChatSchemaInitializer {

    private final WsChatRepository wsChatRepository;

    public WsChatSchemaInitializer(WsChatRepository wsChatRepository) {
        this.wsChatRepository = wsChatRepository;
    }

    @PostConstruct
    public void init() {
        wsChatRepository.ensureSchema().block(Duration.ofSeconds(10));
    }
}
