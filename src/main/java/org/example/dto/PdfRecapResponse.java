package org.example.dto;

/**
 * PdfRecapResponse - Data Transfer Object for PDF recap API responses
 * 
 * This record provides a standardized response structure for the PDF recap endpoint,
 * ensuring type safety and consistent API responses. It supports both success and error
 * scenarios through conditional field usage.
 * 
 * Success Response:
 * - All fields populated except 'error' (which is null)
 * - Contains document metadata and AI-generated recap
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
public record PdfRecapResponse(
    /**
     * Response status - "success" for successful processing, "error" for failures
     */
    String status,
    
    /**
     * Original filename of the uploaded PDF
     */
    String filename,
    
    /**
     * File size in bytes
     */
    Long fileSize,
    
    /**
     * Number of pages in the PDF document
     */
    Integer pageCount,
    
    /**
     * AI-generated recap/summary of the document content
     * Structured with executive summary, key points, main topics, and conclusion
     */
    String recap,
    
    /**
     * Number of characters extracted from the PDF after cleaning and truncation
     */
    Integer extractedCharacters,
    
    /**
     * ISO 8601 timestamp of when the request was processed
     */
    String timestamp,
    
    /**
     * Total processing time in milliseconds
     */
    Long processingTimeMs,
    
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
         * Examples: "INVALID_FILE", "PDF_EXTRACTION_FAILED", "FILE_SIZE_EXCEEDED"
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
     * @param filename Original filename
     * @param fileSize File size in bytes
     * @param pageCount Number of pages
     * @param recap AI-generated recap
     * @param extractedCharacters Number of extracted characters
     * @param timestamp Processing timestamp
     * @param processingTimeMs Processing time in milliseconds
     * @return PdfRecapResponse with success status and null error
     */
    public static PdfRecapResponse success(
            String filename,
            Long fileSize,
            Integer pageCount,
            String recap,
            Integer extractedCharacters,
            String timestamp,
            Long processingTimeMs) {
        
        return new PdfRecapResponse(
            "success",
            filename,
            fileSize,
            pageCount,
            recap,
            extractedCharacters,
            timestamp,
            processingTimeMs,
            null  // no error for success response
        );
    }
    
    /**
     * Creates an error response with minimal required fields
     * 
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @return PdfRecapResponse with error status and populated error details
     */
    public static PdfRecapResponse error(String errorCode, String errorMessage, String timestamp) {
        return new PdfRecapResponse(
            "error",
            null,     // filename may not be available
            null,     // fileSize may not be available
            null,     // pageCount may not be available
            null,     // no recap for error response
            null,     // extractedCharacters may not be available
            timestamp,
            null,     // processingTimeMs may not be meaningful for errors
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
    
    /**
     * Creates an error response with some available metadata
     * 
     * Useful when error occurs after some processing has been completed
     * 
     * @param filename Original filename (if available)
     * @param fileSize File size (if available)
     * @param errorCode Application-specific error code
     * @param errorMessage Human-readable error message
     * @param timestamp Error timestamp
     * @return PdfRecapResponse with error status and available metadata
     */
    public static PdfRecapResponse errorWithMetadata(
            String filename,
            Long fileSize,
            String errorCode,
            String errorMessage,
            String timestamp) {
        
        return new PdfRecapResponse(
            "error",
            filename,
            fileSize,
            null,     // pageCount may not be available
            null,     // no recap for error response
            null,     // extractedCharacters may not be available
            timestamp,
            null,     // processingTimeMs may not be meaningful for errors
            new ErrorDetails(errorCode, errorMessage, timestamp)
        );
    }
}