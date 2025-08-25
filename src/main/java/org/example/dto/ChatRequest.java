package org.example.dto;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * ChatRequest - Data Transfer Object for chat message requests
 * 
 * This record provides a standardized request structure for the chat BFF endpoint,
 * supporting both text messages and file uploads. It follows the same patterns as
 * other DTOs in the application while accommodating multipart/form-data requests.
 * 
 * Features:
 * - Required text message field for user communication
 * - Optional file uploads for PDF and image analysis
 * - Optional conversation ID for future session management
 * 
 * The record structure ensures:
 * - Type safety at compile time
 * - Clear API contract for frontend integration
 * - Consistent validation patterns
 * - Support for multipart form data handling
 */
public record ChatRequest(
    /**
     * The user's text message (required)
     * This is the primary communication from the user
     * Maximum length: 2000 characters (configured in application.properties)
     */
    String message,
    
    /**
     * Optional list of uploaded files (PDFs, images)
     * These files will be acknowledged in Step 1 and processed in future steps
     * Supported formats: PDF, JPG, PNG, etc.
     * Maximum files per message: 5 (configured in application.properties)
     */
    List<MultipartFile> files,
    
    /**
     * Optional conversation ID for session tracking
     * If not provided, a new UUID will be generated
     * Used for maintaining conversation context in future implementations
     */
    String conversationId
) {
    
    /**
     * Validates that the request has at least a message
     * 
     * @return true if the request has a non-empty message
     */
    public boolean hasMessage() {
        return message != null && !message.trim().isEmpty();
    }
    
    /**
     * Checks if the request includes file uploads
     * 
     * @return true if files are present and not empty
     */
    public boolean hasFiles() {
        return files != null && !files.isEmpty();
    }
    
    /**
     * Gets the number of uploaded files
     * 
     * @return count of uploaded files or 0 if none
     */
    public int getFileCount() {
        return hasFiles() ? files.size() : 0;
    }
    
    /**
     * Checks if a conversation ID is provided
     * 
     * @return true if conversationId is present and not empty
     */
    public boolean hasConversationId() {
        return conversationId != null && !conversationId.trim().isEmpty();
    }
}