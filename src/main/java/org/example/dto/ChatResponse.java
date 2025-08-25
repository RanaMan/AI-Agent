package org.example.dto;

import java.util.List;
import java.util.ArrayList;

/**
 * ChatResponse - Data Transfer Object for chat message API responses
 * 
 * This record provides a standardized response structure for the chat BFF endpoint,
 * ensuring type safety and consistent API responses. It supports both success and error
 * scenarios through conditional field usage, following the established patterns from
 * PdfRecapResponse and EmailResponse.
 * 
 * Success Response:
 * - All fields populated except 'error' (which is null)
 * - Contains chat response text and metadata about processing
 * - Lists tools that were used (empty for Step 1, populated in future steps)
 * 
 * Error Response:
 * - 'error' field populated with error details
 * - Some fields may be null depending on where the error occurred
 * - 'status' field indicates "error"
 * 
 * Benefits:
 * - Type safety at compile time
 * - Clear API contract for React frontend
 * - Consistent error handling across the application
 * - Support for conversation tracking and tool execution audit
 */
public record ChatResponse(
    /**
     * Response status - "success" for successful processing, "error" for failures
     */
    String status,
    
    /**
     * The chat response text
     * For Step 1: hardcoded responses based on message patterns
     * For future steps: Claude AI generated responses
     */
    String response,
    
    /**
     * Conversation ID for session tracking
     * Either provided in request or newly generated UUID
     */
    String conversationId,
    
    /**
     * ISO 8601 timestamp of when the request was processed
     */
    String timestamp,
    
    /**
     * Total processing time in milliseconds
     * Includes all processing: validation, file handling, response generation
     */
    Long processingTimeMs,
    
    /**
     * List of tools/APIs that were called during processing
     * Empty list for Step 1 (hardcoded responses)
     * Will contain: "pdf_analyzer", "image_analyzer", "email_sender" in future steps
     */
    List<String> toolsUsed,
    
    /**
     * Error details - null for successful responses, populated for errors
     */
    ErrorDetails error
) {
    
    /**
     * ErrorDetails - Nested record containing detailed error information
     * 
     * Provides structured error information with consistent formatting
     * across all error scenarios, following the same pattern as other DTOs.
     */
    public record ErrorDetails(
        /**
         * Application-specific error code for programmatic handling
         * Examples: "INVALID_MESSAGE", "FILE_VALIDATION_FAILED", "PROCESSING_ERROR"
         */
        String code,
        
        /**
         * Human-readable error message describing what went wrong
         */
        String message,
        
        /**
         * ISO 8601 timestamp of when the error occurred
         */
        String timestamp
    ) {}
    
    /**
     * Creates a successful response with all required fields
     * 
     * @param response The chat response text
     * @param conversationId Conversation ID for tracking
     * @param timestamp Processing timestamp
     * @param processingTimeMs Processing time in milliseconds
     * @param toolsUsed List of tools that were called
     * @return ChatResponse with success status and null error
     */
    public static ChatResponse success(
            String response,
            String conversationId,
            String timestamp,
            Long processingTimeMs,
            List<String> toolsUsed) {
        
        return new ChatResponse(
            "success",
            response,
            conversationId,
            timestamp,
            processingTimeMs,
            toolsUsed != null ? toolsUsed : new ArrayList<>(),
            null  // no error for success response
        );
    }
    
    /**
     * Creates a successful response with no tools used (for Step 1)
     * 
     * @param response The chat response text
     * @param conversationId Conversation ID for tracking
     * @param timestamp Processing timestamp
     * @param processingTimeMs Processing time in milliseconds
     * @return ChatResponse with success status and empty tools list
     */
    public static ChatResponse successWithoutTools(
            String response,
            String conversationId,
            String timestamp,
            Long processingTimeMs) {
        
        return success(response, conversationId, timestamp, processingTimeMs, new ArrayList<>());
    }
    
    /**
     * Creates an error response with minimal required fields
     * 
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @return ChatResponse with error status and populated error details
     */
    public static ChatResponse error(String errorCode, String errorMessage, String timestamp) {
        return new ChatResponse(
            "error",
            null,     // no response text for errors
            null,     // conversationId may not be available
            timestamp,
            null,     // processingTimeMs may not be meaningful for errors
            new ArrayList<>(),  // no tools used for errors
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
    
    /**
     * Creates an error response with some available metadata
     * 
     * Useful when error occurs after some processing has been completed
     * 
     * @param conversationId Conversation ID (if available)
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @param processingTimeMs Processing time (if available)
     * @return ChatResponse with error status and available metadata
     */
    public static ChatResponse errorWithMetadata(
            String conversationId,
            String errorCode,
            String errorMessage,
            String timestamp,
            Long processingTimeMs) {
        
        return new ChatResponse(
            "error",
            null,  // no response text for errors
            conversationId,
            timestamp,
            processingTimeMs,
            new ArrayList<>(),  // no tools used for errors
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
}