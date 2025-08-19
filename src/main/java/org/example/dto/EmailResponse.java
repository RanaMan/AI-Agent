package org.example.dto;

/**
 * EmailResponse - Data Transfer Object for email sending API responses
 * 
 * This record provides a standardized response structure for the email sending endpoint,
 * ensuring type safety and consistent API responses. It supports both success and error
 * scenarios through conditional field usage, following the same pattern as PdfRecapResponse.
 * 
 * Success Response:
 * - All fields populated except 'error' (which is null)
 * - Contains email details and AWS SES message ID
 * 
 * Error Response:
 * - 'error' field populated with error details
 * - Some fields may be null depending on where the error occurred
 * - 'status' field indicates "error"
 * 
 * Benefits:
 * - Type safety at compile time
 * - Clear API contract for consumers
 * - Easier testing and validation
 * - Better IDE support and documentation
 */
public record EmailResponse(
    /**
     * Response status - "success" for successful sending, "error" for failures
     */
    String status,
    
    /**
     * Recipient email address
     */
    String recipient,
    
    /**
     * Email subject line
     */
    String subject,
    
    /**
     * AWS SES message ID for successful sends
     * Format: <message-id>@<region>.amazonses.com
     */
    String messageId,
    
    /**
     * ISO 8601 timestamp of when the request was processed
     */
    String timestamp,
    
    /**
     * Total processing time in milliseconds
     */
    Long processingTimeMs,
    
    /**
     * Sender email address (from configuration)
     */
    String sender,
    
    /**
     * Error details - null for successful responses, populated for errors
     */
    ErrorDetails error
) {
    
    /**
     * ErrorDetails - Nested record containing detailed error information
     * 
     * Provides structured error information with consistent formatting
     * across all error scenarios.
     */
    public record ErrorDetails(
        /**
         * Application-specific error code for programmatic handling
         * Examples: "INVALID_EMAIL", "SES_SEND_FAILED", "AUTHENTICATION_FAILED"
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
     * @param recipient Recipient email address
     * @param subject Email subject
     * @param messageId AWS SES message ID
     * @param sender Sender email address
     * @param timestamp Processing timestamp
     * @param processingTimeMs Processing time in milliseconds
     * @return EmailResponse with success status and null error
     */
    public static EmailResponse success(
            String recipient,
            String subject,
            String messageId,
            String sender,
            String timestamp,
            Long processingTimeMs) {
        
        return new EmailResponse(
            "success",
            recipient,
            subject,
            messageId,
            timestamp,
            processingTimeMs,
            sender,
            null  // no error for success response
        );
    }
    
    /**
     * Creates an error response with minimal required fields
     * 
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @return EmailResponse with error status and populated error details
     */
    public static EmailResponse error(String errorCode, String errorMessage, String timestamp) {
        return new EmailResponse(
            "error",
            null,     // recipient may not be available
            null,     // subject may not be available
            null,     // no messageId for error response
            timestamp,
            null,     // processingTimeMs may not be meaningful for errors
            null,     // sender may not be available
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
    
    /**
     * Creates an error response with some available metadata
     * 
     * Useful when error occurs after some processing has been completed
     * 
     * @param recipient Recipient email (if available)
     * @param sender Sender email (if available)
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @param processingTimeMs Processing time (if available)
     * @return EmailResponse with error status and available metadata
     */
    public static EmailResponse errorWithMetadata(
            String recipient,
            String sender,
            String errorCode,
            String errorMessage,
            String timestamp,
            Long processingTimeMs) {
        
        return new EmailResponse(
            "error",
            recipient,
            "Policy Information",  // Default subject
            null,                  // no messageId for error response
            timestamp,
            processingTimeMs,
            sender,
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
}