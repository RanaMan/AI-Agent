package org.example;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AgentExample - Spring Configuration class for AI agent beans
 * 
 * This configuration class provides the TechnicalConsultantAgent as a Spring bean
 * that can be autowired into other components like controllers and services.
 * 
 * @Configuration annotation marks this class as a source of bean definitions
 * for the Spring application context.
 * 
 * Key features:
 * - Creates TechnicalConsultantAgent bean with memory capabilities
 * - Integrates with the AnthropicChatModel (Claude AI)
 * - Provides 20-message conversation history
 * - Enables dependency injection throughout the application
 */
@Configuration
public class AgentExample {
    
    /**
     * Creates and configures the TechnicalConsultantAgent bean
     * 
     * This method builds an AI agent using LangChain4j's AiServices builder,
     * connecting it to the Claude AI model and providing conversation memory.
     * 
     * @param claude The AnthropicChatModel bean (injected from Main.java)
     * @return TechnicalConsultantAgent instance ready for dependency injection
     */
    @Bean
    public TechnicalConsultantAgent technicalConsultantAgent(AnthropicChatModel claude) {
        return AiServices.builder(TechnicalConsultantAgent.class)
                .chatModel(claude)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();
    }
}