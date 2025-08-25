package org.example.service;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.service.AiServices;
import org.example.dto.ChatRequest;
import org.example.service.tools.ChatTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * ClaudeService - Service layer for Claude AI chat interactions with conversation memory
 * 
 * This service provides chat capabilities using Claude AI through LangChain4j, with
 * persistent conversation memory mapped by conversationId. Each conversation maintains
 * its own context window for natural, contextual interactions.
 * 
 * Key features:
 * - Conversation memory management per conversationId
 * - File acknowledgment in chat responses
 * - Comprehensive error handling for API failures
 * - Memory cleanup for expired conversations
 * - Token usage tracking and logging
 * - API key validation with graceful error handling
 */
@Service
public class ClaudeService {

    private static final Logger logger = LoggerFactory.getLogger(ClaudeService.class);
    
    @Autowired
    private AnthropicChatModel claudeModel;
    
    @Autowired
    private ChatTools chatTools;
    
    // Configuration values
    @Value("${claude.api.system-message}")
    private String systemMessage;
    
    @Value("${claude.memory.window-size:20}")
    private int memoryWindowSize;
    
    @Value("${chat.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    // Conversation memory management
    private final Map<String, ConversationMemory> conversationMemories = new ConcurrentHashMap<>();
    private final ScheduledExecutorService memoryCleanupScheduler = Executors.newScheduledThreadPool(1);
    
    // Chat agent with system message
    private ChatAgent chatAgent;
    
    /**
     * Chat agent interface for conversation handling
     */
    public interface ChatAgent {
        String chat(String userMessage);
    }
    
    /**
     * Result wrapper for Claude API responses with tool tracking
     */
    public static class ClaudeResult {
        private final String response;
        private final List<String> toolsUsed;
        
        public ClaudeResult(String response, List<String> toolsUsed) {
            this.response = response;
            this.toolsUsed = toolsUsed != null ? toolsUsed : new ArrayList<>();
        }
        
        public String getResponse() {
            return response;
        }
        
        public List<String> getToolsUsed() {
            return new ArrayList<>(toolsUsed);
        }
    }
    
    /**
     * Wrapper class to track memory with timestamps for cleanup
     */
    private static class ConversationMemory {
        private final MessageWindowChatMemory memory;
        private final Instant createdAt;
        private Instant lastAccessedAt;
        
        public ConversationMemory(int windowSize) {
            this.memory = MessageWindowChatMemory.withMaxMessages(windowSize);
            this.createdAt = Instant.now();
            this.lastAccessedAt = Instant.now();
        }
        
        public MessageWindowChatMemory getMemory() {
            this.lastAccessedAt = Instant.now();
            return memory;
        }
        
        public boolean isExpired(int timeoutMinutes) {
            return ChronoUnit.MINUTES.between(lastAccessedAt, Instant.now()) > timeoutMinutes;
        }
        
        public Instant getLastAccessedAt() {
            return lastAccessedAt;
        }
    }
    
    /**
     * Initialize the service and start memory cleanup scheduler
     */
    @PostConstruct
    public void init() {
        try {
            validateApiConfiguration();
            initializeChatAgent();
            startMemoryCleanupScheduler();
            
            // Run API investigation
            org.example.LangChain4jApiInvestigation.investigateAvailableApis();
            
            logger.info("ClaudeService initialized successfully with memory window size: {}", memoryWindowSize);
        } catch (Exception e) {
            logger.error("Failed to initialize ClaudeService: {}", e.getMessage(), e);
            throw new ClaudeServiceException("CLAUDE_INITIALIZATION_FAILED", 
                "Failed to initialize Claude service: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates API key and model configuration
     */
    private void validateApiConfiguration() {
        String apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new ClaudeServiceException("CLAUDE_API_KEY_INVALID", 
                "ANTHROPIC_API_KEY environment variable is missing or empty");
        }
        
        if (claudeModel == null) {
            throw new ClaudeServiceException("CLAUDE_MODEL_NOT_CONFIGURED", 
                "Claude model bean is not properly configured");
        }
        
        logger.debug("Claude API configuration validated successfully");
    }
    
    /**
     * Initializes the chat agent with system message
     */
    private void initializeChatAgent() {
        try {
            this.chatAgent = AiServices.builder(ChatAgent.class)
                .chatModel(claudeModel)
                .systemMessageProvider(memoryId -> systemMessage)
                .build();
                
            logger.debug("Chat agent initialized with system message (tools available in conversation agents)");
        } catch (Exception e) {
            throw new ClaudeServiceException("CLAUDE_AGENT_INITIALIZATION_FAILED", 
                "Failed to create chat agent: " + e.getMessage(), e);
        }
    }
    
    /**
     * Starts the scheduled task for cleaning up expired conversation memories
     */
    private void startMemoryCleanupScheduler() {
        memoryCleanupScheduler.scheduleWithFixedDelay(
            this::cleanupExpiredMemories,
            sessionTimeoutMinutes, // Initial delay
            sessionTimeoutMinutes / 2, // Run every half timeout period
            TimeUnit.MINUTES
        );
        
        logger.debug("Memory cleanup scheduler started - cleanup every {} minutes", sessionTimeoutMinutes / 2);
    }
    
    /**
     * Processes a chat message with conversation memory, file acknowledgment, and tool tracking
     * 
     * @param request The chat request containing message, files, and conversationId
     * @return ClaudeResult containing response text and tools used
     * @throws ClaudeServiceException if API call fails or configuration is invalid
     */
    public ClaudeResult processMessage(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String conversationId = request.conversationId();
        
        logger.info("Processing chat message for conversation: {} - message length: {}, files: {}", 
                   conversationId, request.message().length(), request.getFileCount());
        
        try {
            // Get or create conversation memory
            MessageWindowChatMemory memory = getOrCreateMemory(conversationId);
            
            // Cache uploaded files for tool access and clear previous tool executions
            Map<String, MultipartFile> conversationFiles = cacheUploadedFiles(request);
            chatTools.setFileCache(conversationFiles);
            chatTools.clearExecutedTools();
            
            // Build the message with file information for tool calling
            String enhancedMessage = buildEnhancedMessage(request);
            
            // Create agent with memory and tools for this conversation
            ChatAgent conversationAgent = AiServices.builder(ChatAgent.class)
                .chatModel(claudeModel)
                .tools(chatTools)
                .chatMemoryProvider(memoryId -> memory)
                .systemMessageProvider(memoryId -> systemMessage)
                .build();
            
            // Send message to Claude
            String response = conversationAgent.chat(enhancedMessage);
            
            // Get tools that were actually executed directly from ChatTools
            List<String> toolsUsed = chatTools.getExecutedTools();
            logger.debug("Tools executed during this request: {}", toolsUsed);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.info("Claude response generated successfully for conversation: {} - processing time: {}ms, tools used: {}", 
                       conversationId, processingTime, toolsUsed);
            
            // Log response metadata (without sensitive content)
            logger.debug("Response length: {} characters", response.length());
            
            return new ClaudeResult(response, toolsUsed);
            
        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            logger.error("Claude API call failed for conversation: {} - processing time: {}ms - error: {}", 
                        conversationId, processingTime, e.getMessage(), e);
            
            // Convert to appropriate service exception
            throw convertToServiceException(e, conversationId);
        }
    }
    
    /**
     * Gets existing memory or creates new one for the conversation
     */
    private MessageWindowChatMemory getOrCreateMemory(String conversationId) {
        ConversationMemory convMemory = conversationMemories.computeIfAbsent(conversationId, 
            k -> {
                logger.debug("Creating new conversation memory for ID: {}", conversationId);
                return new ConversationMemory(memoryWindowSize);
            });
        
        return convMemory.getMemory();
    }
    
    /**
     * Caches uploaded files using standardized file paths for tool access
     * 
     * @param request The chat request containing uploaded files
     * @return Map of standardized file paths to MultipartFile objects
     */
    private Map<String, MultipartFile> cacheUploadedFiles(ChatRequest request) {
        Map<String, MultipartFile> fileCache = new ConcurrentHashMap<>();
        
        if (request.hasFiles()) {
            for (MultipartFile file : request.files()) {
                // Create standardized path: /uploaded/{conversationId}/{filename}
                String standardizedPath = String.format("/uploaded/%s/%s", 
                    request.conversationId(), file.getOriginalFilename());
                
                fileCache.put(standardizedPath, file);
                
                logger.debug("Cached file for tools: {} -> {} (size: {} bytes)", 
                           standardizedPath, file.getOriginalFilename(), file.getSize());
            }
        }
        
        return fileCache;
    }
    
    /**
     * Builds enhanced message with file information for tool calling
     */
    private String buildEnhancedMessage(ChatRequest request) {
        StringBuilder messageBuilder = new StringBuilder(request.message());
        
        if (request.hasFiles()) {
            messageBuilder.append("\n\n[UPLOADED FILES AVAILABLE FOR ANALYSIS:");
            
            for (MultipartFile file : request.files()) {
                String filename = file.getOriginalFilename();
                String contentType = file.getContentType();
                
                // Create standardized file path that tools can reference
                String filePath = String.format("/uploaded/%s/%s", 
                    request.conversationId(), filename);
                
                messageBuilder.append("\n- File: ").append(filename)
                             .append(" (").append(contentType).append(")")
                             .append(" - Available at path: ").append(filePath);
            }
            
            messageBuilder.append("\nYou can use your analysis tools on these files if the user requests analysis.]");
            
            logger.debug("Enhanced message with {} files prepared for tool calling", request.files().size());
        }
        
        return messageBuilder.toString();
    }
    
    /**
     * Detects tool usage from Claude's response by analyzing actual execution markers
     * 
     * @param response Claude's response text
     * @return List of tool names that were called
     */
    private List<String> detectToolUsage(String response) {
        List<String> toolsUsed = new ArrayList<>();
        
        if (response == null) {
            return toolsUsed;
        }
        
        // Check for actual execution markers that indicate tool usage
        String lowerResponse = response.toLowerCase();
        
        // Check for PDF analysis tool (look for actual results, not mock)
        if (lowerResponse.contains("pdf analysis results:") || 
            lowerResponse.contains("document details:") ||
            (lowerResponse.contains("pdf") && 
             (lowerResponse.contains("pages:") || lowerResponse.contains("characters extracted:")))) {
            toolsUsed.add("analyze_pdf");
            logger.debug("Detected PDF analysis tool usage in response");
        }
        
        // Check for image analysis tool (look for actual results)
        if (lowerResponse.contains("image analysis results:") ||
            (lowerResponse.contains("image") && lowerResponse.contains("analysis completed successfully"))) {
            toolsUsed.add("analyze_image");
            logger.debug("Detected image analysis tool usage in response");
        }
        
        // Check for email sending tool (look for actual delivery confirmation)
        if (lowerResponse.contains("policy information email sent successfully") ||
            lowerResponse.contains("aws ses message id:") ||
            lowerResponse.contains("email delivery confirmed")) {
            toolsUsed.add("send_policy_email");
            logger.debug("Detected policy email tool usage in response");
        }
        
        return toolsUsed;
    }
    
    /**
     * Creates a description of an uploaded file
     */
    private String describeFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        long size = file.getSize();
        
        String typeDescription;
        if (contentType != null && contentType.startsWith("application/pdf")) {
            typeDescription = "PDF document";
        } else if (contentType != null && contentType.startsWith("image/")) {
            typeDescription = "image file";
        } else {
            typeDescription = "file";
        }
        
        return String.format("%s (%s, %d bytes)", filename, typeDescription, size);
    }
    
    /**
     * Converts exceptions to appropriate ClaudeServiceException with function calling support
     */
    private ClaudeServiceException convertToServiceException(Exception e, String conversationId) {
        String message = e.getMessage();
        
        if (message == null) {
            return new ClaudeServiceException("CLAUDE_API_ERROR", "Unknown error occurred during API call", e);
        }
        
        String lowerMessage = message.toLowerCase();
        
        // Tool execution specific errors
        if (lowerMessage.contains("pdf") && (lowerMessage.contains("processing") || lowerMessage.contains("extraction"))) {
            return new ClaudeServiceException("PDF_PROCESSING_ERROR", 
                "PDF analysis failed: " + message, e);
        }
        
        if (lowerMessage.contains("image") && lowerMessage.contains("analysis")) {
            return new ClaudeServiceException("IMAGE_PROCESSING_ERROR", 
                "Image analysis failed: " + message, e);
        }
        
        if (lowerMessage.contains("email") && lowerMessage.contains("send")) {
            return new ClaudeServiceException("EMAIL_SENDING_ERROR", 
                "Email delivery failed: " + message, e);
        }
        
        if (lowerMessage.contains("file") && (lowerMessage.contains("access") || lowerMessage.contains("not found"))) {
            return new ClaudeServiceException("FILE_ACCESS_ERROR", 
                "File access failed: " + message, e);
        }
        
        // Function calling specific errors
        if (lowerMessage.contains("function") || lowerMessage.contains("tool")) {
            if (lowerMessage.contains("parse") || lowerMessage.contains("parsing")) {
                return new ClaudeServiceException("CLAUDE_FUNCTION_PARSING_ERROR", 
                    "Failed to parse function call response: " + message, e);
            }
            if (lowerMessage.contains("parameter") || lowerMessage.contains("validation")) {
                return new ClaudeServiceException("CLAUDE_FUNCTION_VALIDATION_ERROR", 
                    "Function parameters validation failed: " + message, e);
            }
            if (lowerMessage.contains("mixed") || lowerMessage.contains("response")) {
                return new ClaudeServiceException("CLAUDE_MIXED_RESPONSE_ERROR", 
                    "Error handling mixed text/function response: " + message, e);
            }
        }
        
        // API key related errors
        if (lowerMessage.contains("unauthorized") || lowerMessage.contains("invalid api key") || 
            lowerMessage.contains("authentication")) {
            return new ClaudeServiceException("CLAUDE_API_KEY_INVALID", 
                "Invalid or expired API key", e);
        }
        
        // Rate limiting
        if (lowerMessage.contains("rate limit") || lowerMessage.contains("too many requests")) {
            return new ClaudeServiceException("CLAUDE_API_RATE_LIMIT", 
                "API rate limit exceeded. Please try again later", e);
        }
        
        // Timeout errors
        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return new ClaudeServiceException("CLAUDE_API_TIMEOUT", 
                "Request timed out. Please try again", e);
        }
        
        // Service unavailable
        if (lowerMessage.contains("service unavailable") || lowerMessage.contains("server error") ||
            lowerMessage.contains("503") || lowerMessage.contains("502")) {
            return new ClaudeServiceException("CLAUDE_SERVICE_UNAVAILABLE", 
                "Claude service is temporarily unavailable", e);
        }
        
        // Generic API error
        return new ClaudeServiceException("CLAUDE_API_ERROR", 
            "Claude API call failed: " + message, e);
    }
    
    /**
     * Cleans up expired conversation memories
     */
    private void cleanupExpiredMemories() {
        try {
            int initialSize = conversationMemories.size();
            
            conversationMemories.entrySet().removeIf(entry -> {
                boolean expired = entry.getValue().isExpired(sessionTimeoutMinutes);
                if (expired) {
                    logger.debug("Cleaning up expired memory for conversation: {}", entry.getKey());
                }
                return expired;
            });
            
            int removedCount = initialSize - conversationMemories.size();
            if (removedCount > 0) {
                logger.info("Cleaned up {} expired conversation memories. Active conversations: {}", 
                           removedCount, conversationMemories.size());
            }
            
        } catch (Exception e) {
            logger.warn("Error during memory cleanup: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Gets the count of active conversations
     */
    public int getActiveConversationCount() {
        return conversationMemories.size();
    }
    
    /**
     * Manually clears all conversation memories (for testing or admin purposes)
     */
    public void clearAllMemories() {
        int count = conversationMemories.size();
        conversationMemories.clear();
        logger.info("Cleared {} conversation memories", count);
    }
    
    /**
     * Shutdown hook to clean up resources
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ClaudeService...");
        memoryCleanupScheduler.shutdown();
        try {
            if (!memoryCleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                memoryCleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            memoryCleanupScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("ClaudeService shutdown complete");
    }
    
    /**
     * Custom exception class for Claude service errors
     */
    public static class ClaudeServiceException extends RuntimeException {
        private final String errorCode;
        
        public ClaudeServiceException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public ClaudeServiceException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}