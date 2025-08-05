package org.example;

// Import statement for MessageWindowChatMemory - this manages the conversation history
// It keeps track of previous messages so the AI can maintain context across multiple interactions
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

// Import for AnthropicChatModel - this is the specific AI model implementation
// Anthropic is the company that makes Claude AI, and this class interfaces with their API
import dev.langchain4j.model.anthropic.AnthropicChatModel;

// Import for AiServices - this is a LangChain4j utility that creates AI service implementations
// It acts as a factory that builds AI agents based on interfaces we define
import dev.langchain4j.service.AiServices;

// Spring imports for dependency injection and lifecycle management
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;

/**
 * AgentExample class demonstrates how to use LangChain4j to create and interact with an AI agent.
 * This class acts as a wrapper around the TechnicalConsultantAgent interface,
 * providing concrete examples of how to use the AI agent for various tasks.
 * 
 * @Service annotation marks this class as a Spring service component, which means:
 * - Spring will automatically create an instance of this class
 * - It can be injected into other Spring components
 * - It's a singleton by default
 * 
 * The main purpose is to show how to:
 * 1. Build an AI agent with memory capabilities
 * 2. Use the agent for problem analysis
 * 3. Use the agent for code review
 * 4. Have follow-up conversations with context
 */
@Service
public class AgentExample {
    // This field holds our AI agent instance that will handle all AI interactions
    // It's not final anymore because we'll initialize it in @PostConstruct
    // The agent is of type TechnicalConsultantAgent, which is our custom interface
    private TechnicalConsultantAgent agent;
    
    // The Claude model will be injected by Spring
    private final AnthropicChatModel claude;

    /**
     * Constructor that uses Spring's dependency injection.
     * 
     * @Autowired tells Spring to automatically inject the AnthropicChatModel bean
     * that we defined in Main.java
     * 
     * @param claude - The AnthropicChatModel instance that connects to Claude AI
     *                 This is automatically injected by Spring from the @Bean in Main.java
     */
    @Autowired
    public AgentExample(AnthropicChatModel claude) {
        this.claude = claude;
    }
    
    /**
     * Initialize the agent after Spring has completed dependency injection.
     * 
     * @PostConstruct ensures this method runs after the bean is fully constructed
     * but before it's put into service. This is where we build our AI agent.
     */
    @PostConstruct
    public void init() {
        // Build our AI agent using the AiServices builder pattern
        this.agent = AiServices.builder(TechnicalConsultantAgent.class)
                .chatModel(claude)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .build();
        
        // Run the demonstration automatically when the application starts
        System.out.println("\n=== Spring Boot Application Started ===");
        demonstrateAgent();
    }

    /**
     * This method demonstrates various capabilities of the AI agent.
     * It shows three different use cases:
     * 1. Analyzing a technical problem
     * 2. Reviewing code
     * 3. Having a follow-up conversation
     * 
     * This method doesn't return anything (void) - it just prints results to the console
     */
    public void demonstrateAgent() {
        // First demonstration: Problem Analysis
        // Print a header to make the output organized and readable
        System.out.println("=== PROBLEM ANALYSIS ===");
        
        // Define a sample problem description that simulates a real-world technical issue
        // This is a typical scenario a developer might face with a Spring Boot application
        // The + operator concatenates the two string parts into one longer string
        String problem = "Our Spring Boot application is experiencing high latency during peak traffic. " +
                "Database queries are slow and we're seeing connection pool exhaustion.";
        
        // Call the analyzeProblem method on our agent, passing in the problem description
        // The agent will process this and return an analysis as a String
        // This method is defined in the TechnicalConsultantAgent interface
        String analysis = agent.analyzeProblem(problem);
        
        // Print the AI's analysis to the console so we can see the response
        System.out.println(analysis);

        // Second demonstration: Code Review
        // Print another header to separate this section from the previous one
        System.out.println("\n=== CODE REVIEW ===");
        
        // Define a code snippet for review using Java's text block feature (""")
        // Text blocks allow us to write multi-line strings without escape characters
        // This represents a simple Spring Boot REST controller that might need review
        String codeToReview = """
                @RestController
                public class UserController {
                    @Autowired
                    private UserRepository userRepository;
                    
                    @GetMapping("/users")
                    public List<User> getAllUsers() {
                        return userRepository.findAll();
                    }
                }
                """;
        
        // Reuse the analyzeProblem method to review the code
        // Even though the method is called "analyzeProblem", it can handle code review too
        // because the AI understands context from the input
        String codeReview = agent.analyzeProblem(codeToReview);
        
        // Print the code review results
        System.out.println(codeReview);

        // Third demonstration: Follow-up conversation
        // Print header for the chat section
        System.out.println("\n=== FOLLOW-UP CHAT ===");
        
        // Use the chat method for a follow-up question
        // Because we configured the agent with memory (MessageWindowChatMemory),
        // it remembers the previous conversation about the problem and can provide
        // context-aware recommendations
        String followUp = agent.chat("What specific monitoring tools would you recommend?");
        
        // Print the AI's response about monitoring tools
        // The response should be relevant to the Spring Boot latency problem discussed earlier
        System.out.println(followUp);
    }
}