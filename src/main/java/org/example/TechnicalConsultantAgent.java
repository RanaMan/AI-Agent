package org.example;

// Import for SystemMessage annotation - this defines the AI's role/persona
// It tells the AI how to behave and what expertise to demonstrate
import dev.langchain4j.service.SystemMessage;

// Import for UserMessage annotation - this defines the template for user inputs
// It structures how user messages are formatted before being sent to the AI
import dev.langchain4j.service.UserMessage;

// Import for V annotation - this is for variable binding
// The V stands for "Variable" and it maps method parameters to template placeholders
import dev.langchain4j.service.V;

/**
 * TechnicalConsultantAgent Interface
 * 
 * This is an INTERFACE, not a class. In Java, an interface defines a contract -
 * it specifies what methods must exist, but not how they're implemented.
 * 
 * This interface is special because it uses LangChain4j annotations to define
 * an AI agent's behavior. LangChain4j will automatically create an implementation
 * of this interface that connects to an AI model (like Claude).
 * 
 * Key concepts:
 * 1. This interface acts as a blueprint for an AI agent
 * 2. Methods in this interface become AI prompts
 * 3. Annotations configure how the AI should respond
 * 4. LangChain4j generates the actual implementation using AiServices.builder()
 * 
 * When you call methods on this interface, LangChain4j:
 * 1. Takes your input
 * 2. Formats it according to the annotations
 * 3. Sends it to the AI model
 * 4. Returns the AI's response as a String
 */
public interface TechnicalConsultantAgent {
    
    /**
     * analyzeProblem method - Analyzes technical problems or code snippets
     * 
     * This method has TWO annotations that work together:
     * 
     * @SystemMessage annotation:
     * - Sets the AI's persona/role for this specific method
     * - The "..." means the full message is defined elsewhere (likely in configuration)
     * - This message stays the same for every call to this method
     * - It might say something like "You are a senior technical consultant with 
     *   expertise in Java, Spring Boot, databases, and system architecture"
     * - This gives the AI context about how to respond
     * 
     * @UserMessage annotation:
     * - Defines a template for the user's input
     * - "Analyze this problem: {{problem}}" is the template
     * - {{problem}} is a placeholder that gets replaced with actual input
     * - This structures the prompt in a way that gets better AI responses
     * 
     * @V annotation on the parameter:
     * - Maps the method parameter to the template placeholder
     * - @V("problem") means: take the 'problem' parameter and put it where {{problem}} is
     * 
     * How it works when called:
     * 1. You call: agent.analyzeProblem("My database is slow")
     * 2. LangChain4j creates prompt: "Analyze this problem: My database is slow"
     * 3. Adds system message about being a technical consultant
     * 4. Sends to AI and returns the response
     * 
     * @param problem The technical problem or code to analyze
     * @return The AI's analysis as a String
     */
    @SystemMessage("You are a senior technical consultant...")
    @UserMessage("Analyze this problem: {{problem}}")
    String analyzeProblem(@V("problem") String problem);

    /**
     * chat method - General conversation with the AI agent
     * 
     * This method has NO annotations, which means:
     * - No special system message (uses default from agent configuration)
     * - No template formatting (sends userMessage directly to AI)
     * - This is for free-form conversation
     * 
     * The comment "Remove the @UserMessage annotation entirely" suggests this
     * method previously had an annotation that was causing issues. Without
     * the annotation, the message is sent as-is to the AI.
     * 
     * This method is useful for:
     * - Follow-up questions
     * - Clarifications
     * - General discussion
     * - Any interaction that doesn't need special formatting
     * 
     * Because the agent has memory (configured in AgentExample), it remembers
     * previous conversations when you use this method.
     * 
     * @param userMessage Any message or question for the AI
     * @return The AI's response as a String
     */
    String chat(String userMessage);  // Remove the @UserMessage annotation entirely

    /**
     * recapDocument method - Generates a structured recap/summary of document text
     * 
     * This method is specifically designed for comprehensive document analysis and summarization.
     * It uses an enhanced system message to configure the AI as a professional document analyst
     * who provides structured, well-organized recaps with consistent formatting.
     * 
     * @SystemMessage annotation:
     * - Configures the AI's role as a professional document analyst
     * - Instructs the AI to provide structured content with:
     *   1) Brief executive summary (2-3 sentences)
     *   2) Key points in bullet format
     *   3) Main topics covered
     *   4) Conclusion
     * - Ensures consistent, comprehensive yet concise responses
     * 
     * @UserMessage annotation:
     * - Uses a template that asks the AI to recap the provided document text
     * - {{documentText}} placeholder gets replaced with the actual document content
     * 
     * @param documentText The extracted text from a document (PDF, etc.)
     * @return A structured recap/summary with executive summary, key points, topics, and conclusion
     */
    @SystemMessage("You are a professional document analyst. When recapping documents, provide: 1) A brief executive summary (2-3 sentences), 2) Key points (bullet format), 3) Main topics covered, and 4) A conclusion. Be concise but comprehensive.")
    @UserMessage("Recap this document please: {{documentText}}")
    String recapDocument(@V("documentText") String documentText);
}