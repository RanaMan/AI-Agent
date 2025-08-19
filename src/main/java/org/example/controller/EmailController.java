package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.EmailRequest;
import org.example.dto.EmailResponse;
import org.example.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * EmailController - REST controller for sending policy information emails
 * 
 * This controller provides endpoints for sending formatted HTML emails containing
 * policy information via AWS SES. It follows the same error handling and response
 * patterns as the PdfProcessorController.
 * 
 * Key features:
 * - AWS SES integration for email delivery
 * - HTML formatted emails with policy details
 * - Comprehensive error handling
 * - Structured JSON responses
 * - Detailed Swagger documentation
 */
@RestController
@RequestMapping("/api/email")
@Tag(name = "Email Service", description = "API for sending policy information emails via AWS SES")
public class EmailController {

    private static final Logger logger = LoggerFactory.getLogger(EmailController.class);
    
    @Autowired
    private EmailService emailService;
    
    @Value("${aws.ses.sender.email}")
    private String senderEmail;

    /**
     * POST endpoint for sending policy information emails
     * 
     * Accepts a JSON payload with customer and policy details,
     * formats it into an HTML email, and sends it via AWS SES.
     * 
     * @param request EmailRequest containing recipient and policy details
     * @return ResponseEntity with success/error JSON response
     */
    @PostMapping(value = "/send", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Send policy information email",
        description = "Send an HTML formatted email containing policy information to the specified recipient using AWS SES. The email includes customer details, policy number, and VIN in a professionally formatted table."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email processed (success or error details in response body)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = EmailResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        value = """
                        {
                          "status": "success",
                          "recipient": "customer@example.com",
                          "subject": "Policy Information",
                          "messageId": "<01000190abc12345-a1b2c3d4-e5f6-7890-abcd-ef1234567890-000000@us-east-1.amazonses.com>",
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": 1250,
                          "sender": "noreply@example.com",
                          "error": null
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Invalid Email Error",
                        value = """
                        {
                          "status": "error",
                          "recipient": null,
                          "subject": null,
                          "messageId": null,
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": null,
                          "sender": null,
                          "error": {
                            "code": "INVALID_EMAIL",
                            "message": "Invalid email address format: not-an-email",
                            "timestamp": "2025-01-07T10:30:00Z"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "SES Send Failed Error",
                        value = """
                        {
                          "status": "error",
                          "recipient": "customer@example.com",
                          "subject": "Policy Information",
                          "messageId": null,
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": 500,
                          "sender": "noreply@example.com",
                          "error": {
                            "code": "SES_SEND_FAILED",
                            "message": "Failed to send email via AWS SES: Email address is not verified",
                            "timestamp": "2025-01-07T10:30:00Z"
                          }
                        }
                        """
                    ),
                    @ExampleObject(
                        name = "Authentication Failed Error",
                        value = """
                        {
                          "status": "error",
                          "recipient": null,
                          "subject": null,
                          "messageId": null,
                          "timestamp": "2025-01-07T10:30:00Z",
                          "processingTimeMs": null,
                          "sender": null,
                          "error": {
                            "code": "AUTHENTICATION_FAILED",
                            "message": "AWS SES authentication failed. Please check AWS credentials",
                            "timestamp": "2025-01-07T10:30:00Z"
                          }
                        }
                        """
                    )
                }
            )
        )
    })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Email request payload containing recipient and policy details",
        required = true,
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = EmailRequest.class),
            examples = @ExampleObject(
                name = "Example Request",
                value = """
                {
                  "emailAddress": "customer@example.com",
                  "firstName": "John",
                  "lastName": "Doe",
                  "policyNumber": "POL-123456",
                  "vin": "1HGBH41JXMN109186"
                }
                """
            )
        )
    )
    public ResponseEntity<EmailResponse> sendEmail(@RequestBody EmailRequest request) {
        
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();
        
        logger.info("Email send request received - recipient: {}, policy: {}", 
                   request.emailAddress(), request.policyNumber());

        try {
            // Validate request
            validateEmailRequest(request);
            
            // Send email via service
            String messageId = emailService.sendPolicyEmail(request);
            
            // Calculate total processing time
            long totalProcessingTime = System.currentTimeMillis() - startTime;
            
            // Build success response using DTO
            EmailResponse response = EmailResponse.success(
                request.emailAddress(),
                "Policy Information",
                messageId,
                senderEmail,
                timestamp,
                totalProcessingTime
            );
            
            logger.info("Email sent successfully to {} - Message ID: {} - Processing time: {}ms", 
                       request.emailAddress(), messageId, totalProcessingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (EmailService.EmailSendException e) {
            long processingTime = System.currentTimeMillis() - startTime;
            
            logger.error("Email sending failed: {} - {}", e.getErrorCode(), e.getMessage());
            
            // Determine if we have enough metadata for error response
            if (e.getErrorCode().equals("INVALID_EMAIL") || e.getErrorCode().equals("VALIDATION_FAILED")) {
                // Validation errors - minimal metadata
                return ResponseEntity.ok(
                    EmailResponse.error(
                        e.getErrorCode(),
                        e.getMessage(),
                        timestamp
                    )
                );
            } else {
                // Processing errors - include available metadata
                return ResponseEntity.ok(
                    EmailResponse.errorWithMetadata(
                        request.emailAddress(),
                        senderEmail,
                        e.getErrorCode(),
                        e.getMessage(),
                        timestamp,
                        processingTime
                    )
                );
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request data: {}", e.getMessage());
            return ResponseEntity.ok(
                EmailResponse.error(
                    "VALIDATION_FAILED",
                    e.getMessage(),
                    timestamp
                )
            );
            
        } catch (Exception e) {
            logger.error("Unexpected error during email sending: {}", e.getMessage(), e);
            
            // Check for specific AWS authentication issues
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("Authentication") || 
                                        errorMessage.contains("Credential") ||
                                        errorMessage.contains("AWS"))) {
                return ResponseEntity.ok(
                    EmailResponse.error(
                        "AUTHENTICATION_FAILED",
                        "AWS SES authentication failed. Please check AWS credentials",
                        timestamp
                    )
                );
            }
            
            return ResponseEntity.ok(
                EmailResponse.error(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred while sending the email",
                    timestamp
                )
            );
        }
    }

    /**
     * Validates the email request
     * 
     * Checks:
     * - All required fields are present
     * - Fields are not empty or blank
     * - Basic data validation
     * 
     * @param request The email request to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateEmailRequest(EmailRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Email request cannot be null");
        }
        
        // Validate email address
        if (request.emailAddress() == null || request.emailAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Email address is required");
        }
        
        // Validate first name
        if (request.firstName() == null || request.firstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        // Validate last name
        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }
        
        // Validate policy number
        if (request.policyNumber() == null || request.policyNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Policy number is required");
        }
        
        // Validate VIN
        if (request.vin() == null || request.vin().trim().isEmpty()) {
            throw new IllegalArgumentException("VIN is required");
        }
        
        // Validate VIN length (should be 17 characters for standard VINs)
        if (request.vin().length() != 17) {
            logger.warn("Non-standard VIN length: {} characters for VIN: {}", 
                       request.vin().length(), request.vin());
        }
        
        logger.debug("Email request validation passed for recipient: {}", request.emailAddress());
    }
}