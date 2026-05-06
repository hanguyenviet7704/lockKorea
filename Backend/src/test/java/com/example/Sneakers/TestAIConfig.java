package com.example.Sneakers;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Test configuration to provide a dummy ChatModel bean when AI is disabled.
 */
@Configuration
public class TestAIConfig {

    @Bean
    public ChatModel chatModel() {
        // Return a simple mock/stub implementation for tests
        return new ChatModel() {
            // Implement only the methods that might be called in tests
            // Since AI controllers are not being tested, we can provide a minimal stub
        };
    }
}
