package org.example;

// Import the AnthropicChatModel class from LangChain4j
// This class is responsible for connecting to and communicating with Anthropic's Claude AI API
// LangChain4j is a Java framework that simplifies working with Large Language Models (LLMs)
import dev.langchain4j.model.anthropic.AnthropicChatModel;

/**
 * Main class - This is the entry point of the application.
 * When you run this Java program, the JVM (Java Virtual Machine) looks for the main() method
 * in this class and executes it first.
 * 
 * This class is responsible for:
 * 1. Setting up the connection to Claude AI (Anthropic's AI model)
 * 2. Creating an instance of AgentExample
 * 3. Running the agent demonstration
 */
public class Main {
    /**
     * The main method - This is where the program starts executing.
     * 
     * The signature "public static void main(String[] args)" is special in Java:
     * - public: means this method can be accessed from anywhere
     * - static: means you don't need to create an instance of Main to run this method
     * - void: means this method doesn't return any value
     * - main: the specific name Java looks for when starting a program
     * - String[] args: an array of command-line arguments (not used in this program)
     * 
     * @param args Command-line arguments passed when running the program (currently unused)
     */
    public static void main(String[] args) {
        // IMPORTANT SECURITY NOTE: 
        // The API key below should NEVER be hardcoded in production code!
        // It's exposed here for demonstration, but in real applications you should:
        // 1. Use environment variables: System.getenv("ANTHROPIC_API_KEY")
        // 2. Use a secure configuration file that's not committed to version control
        // 3. Use a secrets management service
        
        // Create and configure the Claude AI model using the builder pattern
        // The builder pattern allows us to set multiple configuration options in a readable way
       AnthropicChatModel claude = AnthropicChatModel.builder()
                // .apiKey() sets the authentication key for accessing Anthropic's API
                // This key is what authorizes your application to use Claude AI
                // WARNING: This is your actual API key - keep it secret!
                // Each API key has usage limits and is tied to billing
                .apiKey(System.getenv("ANTHROPIC_API_KEY")) // Get from environment variable for security
                
                // .modelName() specifies which version of Claude to use
                // "claude-3-5-sonnet-20241022" is a specific version of Claude 3.5 Sonnet
                // Different models have different capabilities, speeds, and costs
                // Sonnet is balanced between capability and speed
                .modelName("claude-3-5-sonnet-20241022")
                
                // .temperature() controls the randomness/creativity of responses (0.0 to 1.0)
                // 0.0 = very deterministic, same input gives nearly identical output
                // 1.0 = very creative/random, same input can give very different outputs
                // 0.7 = moderately creative, good balance for technical consulting
                .temperature(0.7)
                
                // .maxTokens() sets the maximum length of the AI's response
                // Tokens are pieces of words (roughly 1 token = 0.75 words)
                // 1000 tokens ≈ 750 words, which is enough for detailed technical responses
                // Higher values allow longer responses but cost more and take longer
                .maxTokens(1000)
                
                // .build() finalizes the configuration and creates the AnthropicChatModel instance
                // This actually establishes the connection settings (but doesn't connect yet)
                .build();

        // These commented lines show a simple direct chat with Claude
        // They demonstrate basic usage without the agent framework
        // Uncomment these to test basic Claude connectivity
//
//        String response = claude.chat("Hello! Please introduce yourself as a technical consultant.");
//        System.out.println("Claude says: " + response);
        
        // Print a header to indicate we're starting the agent test
        // The \n creates a newline for better formatting in the console output
        System.out.println("\n=== Testing Agent ===");
        
        // Create an instance of AgentExample, passing in our configured Claude model
        // This creates our AI agent with all the capabilities defined in AgentExample class
        // The agent will use this Claude instance for all its AI interactions
        AgentExample agentDemo = new AgentExample(claude);
        
        // Call the demonstrateAgent() method to run all the demonstrations
        // This will:
        // 1. Analyze a technical problem about Spring Boot performance
        // 2. Review some code for best practices
        // 3. Have a follow-up conversation about monitoring tools
        // All output will be printed to the console
        agentDemo.demonstrateAgent();
    }
}