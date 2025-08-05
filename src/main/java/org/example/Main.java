package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import dev.langchain4j.model.anthropic.AnthropicChatModel;

/**
 * Main class - This is the entry point of the Spring Boot application.
 * 
 * @SpringBootApplication is a convenience annotation that combines:
 * - @Configuration: Tags the class as a source of bean definitions
 * - @EnableAutoConfiguration: Tells Spring Boot to auto-configure based on dependencies
 * - @ComponentScan: Tells Spring to look for components in this package and sub-packages
 */
@SpringBootApplication
public class Main {
    /**
     * The main method - This is where the Spring Boot application starts.
     * SpringApplication.run() bootstraps the Spring application context.
     * 
     * @param args Command-line arguments passed when running the program
     */
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }
    
    /**
     * Claude AI model configuration as a Spring Bean.
     * This bean will be automatically injected wherever AnthropicChatModel is needed.
     * 
     * @Bean annotation tells Spring that this method produces a bean to be managed
     * by the Spring container.
     * 
     * @return Configured AnthropicChatModel instance
     */
    @Bean
    public AnthropicChatModel claudeModel() {
        return AnthropicChatModel.builder()
                // Get API key from environment variable for security
                .apiKey(System.getenv("ANTHROPIC_API_KEY"))
                
                // Specify which version of Claude to use
                .modelName("claude-3-5-sonnet-20241022")
                
                // Set temperature for response creativity (0.0-1.0)
                .temperature(0.7)
                
                // Set maximum tokens for response length
                .maxTokens(1000)
                
                // Build the model instance
                .build();
    }
}