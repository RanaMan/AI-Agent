package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.ChatRequest;
import org.example.dto.ChatResponse;
import org.example.service.ClaudeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ChatController - Backend-for-Frontend (BFF) REST controller for chat interface
 * 
 * This controller serves as the orchestration layer for the chat system, handling
 * user messages and file uploads. In Step 1, it provides hardcoded responses for
 * testing the API contract. Future steps will integrate Claude AI for intelligent
 * processing and tool execution.
 * 
 * Key features:
 * - Text message processing with pattern-based responses
 * - File upload support (PDFs and images)
 * - Conversation session tracking
 * - Comprehensive error handling and validation
 * - Structured JSON responses for React frontend
 * - Detailed Swagger documentation
 * 
 * Future capabilities (Steps 2-5):
 * - Claude AI integration for natural language understanding
 * - Dynamic tool selection and execution
 * - PDF analysis via existing PDF processor
 * - Image analysis capabilities
 * - Email sending via AWS SES
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*") // Configure appropriately for production
@Tag(name = "Chat Service", description = "Backend-for-Frontend API for AI-powered chat interactions")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);
    
    @Autowired
    private ClaudeService claudeService;
    
    // Configuration values from application.properties
    @Value("${chat.max.message.length:2000}")
    private int maxMessageLength;
    
    @Value("${chat.max.files.per.message:5}")
    private int maxFilesPerMessage;
    
    @Value("${chat.session.timeout.minutes:30}")
    private int sessionTimeoutMinutes;
    
    // File validation constants
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_FILE_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    );
    
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
        ".pdf", ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    /**
     * POST endpoint for processing chat messages with optional file uploads
     * 
     * Accepts user messages and files, validates inputs, and returns appropriate
     * responses. In Step 1, responses are hardcoded based on message patterns.
     * 
     * @param message User's text message (required)
     * @param files Optional file uploads (PDFs, images)
     * @param conversationId Optional conversation ID for session tracking
     * @return ResponseEntity with ChatResponse containing success or error details
     */
    @PostMapping(value = "/message", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Process chat message with optional file uploads",
        description = "Send a text message with optional file attachments to the chat BFF. " +
                     "The system will process the message and files, returning an appropriate response. " +
                     "In Step 1, responses are hardcoded for testing. Future steps will integrate Claude AI " +
                     "for intelligent processing and tool execution."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Message processed (success or error details in response body)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = ChatResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Text Message Success",
                        value = """
                        {
                          "status": "success",
                          "response": "Hi! I'm your AI assistant. I can analyze PDFs, images, and send emails.",
                          "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": 150,
                          "toolsUsed": [],
                          "error": null
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "File Upload Success",
                        value = """
                        {
                          "status": "success",
                          "response": "I see you uploaded a PDF file: document.pdf. I'll be able to analyze this once PDF processing is integrated.",
                          "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": 200,
                          "toolsUsed": [],
                          "error": null
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Validation Error",
                        value = """
                        {
                          "status": "error",
                          "response": null,
                          "conversationId": null,
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": null,
                          "toolsUsed": [],
                          "error": {
                            "code": "INVALID_MESSAGE",
                            "message": "Message is required and cannot be empty",
                            "timestamp": "2025-01-07T10:30:00Z"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "File Validation Error",
                        value = """
                        {
                          "status": "error",
                          "response": null,
                          "conversationId": "550e8400-e29b-41d4-a716-446655440000",
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": 50,
                          "toolsUsed": [],
                          "error": {
                            "code": "FILE_VALIDATION_FAILED",
                            "message": "Invalid file type for document.txt. Allowed types: PDF, JPG, PNG",
                            "timestamp": "2025-01-07T10:30:00Z"
                          }
                        }
                        """
                    )
                }
            )
        )
    })
    public ResponseEntity<ChatResponse> processMessage(
            @Parameter(
                description = "User's text message (required, max 2000 characters)",
                required = true,
                examples = {
                    @ExampleObject(name = "Greeting", value = "hello"),
                    @ExampleObject(name = "Help Request", value = "help"),
                    @ExampleObject(name = "PDF Analysis", value = "Can you analyze this PDF for VIN numbers?"),
                    @ExampleObject(name = "Email Request", value = "Send an email with the policy information")
                }
            )
            @RequestParam("message") String message,
            
            @Parameter(
                description = "Optional file uploads (PDFs, images). Max 5 files, 10MB each",
                required = false,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            
            @Parameter(
                description = "Optional conversation ID for session tracking. If not provided, a new UUID will be generated",
                required = false,
                example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();
        
        // Generate or use provided conversation ID
        String sessionId = (conversationId != null && !conversationId.trim().isEmpty()) 
            ? conversationId 
            : UUID.randomUUID().toString();
        
        logger.info("Chat message received - conversationId: {}, message length: {}, files: {}", 
                   sessionId, 
                   message != null ? message.length() : 0,
                   files != null ? files.size() : 0);

        try {
            // Create request object for validation
            ChatRequest request = new ChatRequest(message, files, sessionId);
            
            // Validate the request
            validateChatRequest(request);
            
            // Log file details if present
            if (request.hasFiles()) {
                logFileDetails(request.files());
            }
            
            // Generate response using Claude AI (Step 2B: with tool calling)
            ClaudeService.ClaudeResult result = claudeService.processMessage(request);
            String responseText = result.getResponse();
            List<String> toolsUsed = result.getToolsUsed();
            
            // Calculate processing time
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Build success response with tools used
            ChatResponse response = ChatResponse.success(
                responseText,
                sessionId,
                timestamp,
                processingTime,
                toolsUsed
            );
            
            logger.info("Chat message processed successfully - conversationId: {}, processing time: {}ms", 
                       sessionId, processingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (ValidationException e) {
            logger.warn("Chat request validation failed: {}", e.getMessage());
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            // Determine if we have enough context for metadata
            if (e.hasMetadata()) {
                return ResponseEntity.ok(
                    ChatResponse.errorWithMetadata(
                        sessionId,
                        e.getErrorCode(),
                        e.getMessage(),
                        timestamp,
                        processingTime
                    )
                );
            } else {
                return ResponseEntity.ok(
                    ChatResponse.error(
                        e.getErrorCode(),
                        e.getMessage(),
                        timestamp
                    )
                );
            }
            
        } catch (ClaudeService.ClaudeServiceException e) {
            logger.warn("Claude API call failed: {} - {}", e.getErrorCode(), e.getMessage());
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            return ResponseEntity.ok(
                ChatResponse.errorWithMetadata(
                    sessionId,
                    e.getErrorCode(),
                    e.getMessage(),
                    timestamp,
                    processingTime
                )
            );
            
        } catch (Exception e) {
            logger.error("Unexpected error processing chat message: {}", e.getMessage(), e);
            
            return ResponseEntity.ok(
                ChatResponse.error(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred while processing your message",
                    timestamp
                )
            );
        }
    }

    /**
     * Validates the chat request including message and files
     * 
     * @param request The ChatRequest to validate
     * @throws ValidationException if validation fails
     */
    private void validateChatRequest(ChatRequest request) throws ValidationException {
        // Validate message
        if (!request.hasMessage()) {
            throw new ValidationException("INVALID_MESSAGE", "Message is required and cannot be empty", false);
        }
        
        if (request.message().length() > maxMessageLength) {
            throw new ValidationException(
                "MESSAGE_TOO_LONG", 
                String.format("Message exceeds maximum length of %d characters", maxMessageLength),
                true
            );
        }
        
        // Validate files if present
        if (request.hasFiles()) {
            if (request.getFileCount() > maxFilesPerMessage) {
                throw new ValidationException(
                    "TOO_MANY_FILES",
                    String.format("Maximum %d files allowed per message, but %d were provided", 
                                 maxFilesPerMessage, request.getFileCount()),
                    true
                );
            }
            
            // Validate each file
            for (MultipartFile file : request.files()) {
                validateFile(file);
            }
        }
        
        logger.debug("Chat request validation passed - message length: {}, files: {}", 
                    request.message().length(), request.getFileCount());
    }

    /**
     * Validates an individual file upload
     * 
     * @param file The file to validate
     * @throws ValidationException if file validation fails
     */
    private void validateFile(MultipartFile file) throws ValidationException {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new ValidationException(
                "EMPTY_FILE",
                "Empty file upload detected",
                true
            );
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                "FILE_SIZE_EXCEEDED",
                String.format("File '%s' size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                             file.getOriginalFilename(), file.getSize(), MAX_FILE_SIZE),
                true
            );
        }
        
        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_FILE_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
                "FILE_VALIDATION_FAILED",
                String.format("Invalid file type for %s. Allowed types: PDF, JPG, PNG", 
                             file.getOriginalFilename()),
                true
            );
        }
        
        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = filename.substring(filename.lastIndexOf(".")).toLowerCase();
            if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
                throw new ValidationException(
                    "FILE_VALIDATION_FAILED",
                    String.format("Invalid file extension for %s. Allowed extensions: %s", 
                                 filename, ALLOWED_FILE_EXTENSIONS),
                    true
                );
            }
        }
    }

    /**
     * Logs details about uploaded files for debugging
     * 
     * @param files List of uploaded files
     */
    private void logFileDetails(List<MultipartFile> files) {
        for (MultipartFile file : files) {
            logger.info("File uploaded - name: {}, size: {} bytes, type: {}", 
                       file.getOriginalFilename(), 
                       file.getSize(), 
                       file.getContentType());
        }
    }


    /**
     * Custom exception for validation errors with metadata support
     */
    private static class ValidationException extends Exception {
        private final String errorCode;
        private final boolean hasMetadata;
        
        public ValidationException(String errorCode, String message, boolean hasMetadata) {
            super(message);
            this.errorCode = errorCode;
            this.hasMetadata = hasMetadata;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
        
        public boolean hasMetadata() {
            return hasMetadata;
        }
    }
}