package org.example.service;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import jakarta.annotation.PostConstruct;
import org.example.dto.EmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * EmailService - Service for sending emails via AWS SES API
 * 
 * This service handles email composition and sending using AWS SES API with IAM role
 * authentication. It formats policy information into HTML emails with comprehensive
 * error handling and logging.
 * 
 * Key features:
 * - AWS SES API integration with IAM role authentication
 * - HTML email formatting with inline CSS
 * - Email validation
 * - Comprehensive error handling
 * - Detailed logging for troubleshooting
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    // Email validation pattern (basic RFC 5322 compliance)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@" +
        "(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    // Email subject
    private static final String EMAIL_SUBJECT = "Policy Information";
    
    private AmazonSimpleEmailService sesClient;
    
    @Value("${aws.ses.sender.email}")
    private String senderEmail;
    
    @Value("${aws.ses.sender.name:AI Agent System}")
    private String senderName;
    
    @Value("${aws.region:us-east-1}")
    private String awsRegion;
    
    /**
     * Initializes the AWS SES client and logs configuration at startup
     * This method runs automatically after the service is constructed
     */
    @PostConstruct
    public void initializeSesClient() {
        try {
            // Initialize SES client with IAM role credentials
            sesClient = AmazonSimpleEmailServiceClientBuilder.standard()
                    .withRegion(Regions.fromName(awsRegion))
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .build();
            
            logConfigurationAtStartup();
            
        } catch (Exception e) {
            logger.error("Failed to initialize AWS SES client: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize AWS SES client", e);
        }
    }
    
    /**
     * Logs AWS SES configuration at startup for debugging
     */
    private void logConfigurationAtStartup() {
        logger.info("========================================");
        logger.info("AWS SES Email Service Configuration:");
        logger.info("========================================");
        logger.info("AWS Region: {}", awsRegion);
        logger.info("SES Endpoint: email.{}.amazonaws.com", awsRegion);
        logger.info("Sender Email: {}", senderEmail);
        logger.info("Sender Name: {}", senderName);
        logger.info("AWS Authentication: Using IAM Role (DefaultAWSCredentialsProviderChain)");
        logger.info("API Method: AWS SES API (not SMTP)");
        logger.info("========================================");
        
        // Additional validation logging
        if (senderEmail == null || senderEmail.isEmpty()) {
            logger.warn("WARNING: SES_SENDER_EMAIL environment variable is not set!");
        }
        
        logger.info("Note: Using EC2 instance IAM role for AWS SES API authentication.");
        logger.info("Required IAM permissions: ses:SendEmail, ses:SendRawEmail");
        
        // Test SES client connection
        try {
            GetSendQuotaResult quota = sesClient.getSendQuota();
            logger.info("SES Send Quota: {}/{} emails per 24 hours", 
                       quota.getSentLast24Hours().intValue(), quota.getMax24HourSend().intValue());
        } catch (Exception e) {
            logger.warn("Could not retrieve SES send quota (may indicate permission issues): {}", e.getMessage());
        }
    }

    /**
     * Sends an email with policy information formatted as HTML
     * 
     * @param request EmailRequest containing recipient and policy details
     * @return AWS SES message ID if successful
     * @throws EmailSendException if sending fails
     */
    public String sendPolicyEmail(EmailRequest request) throws EmailSendException {
        logger.info("Processing email request for recipient: {}", request.emailAddress());
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Validate email address
            validateEmailAddress(request.emailAddress());
            
            // Log configuration before attempting to send
            logger.info("Attempting to send email with the following configuration:");
            logger.info("  - From: {} ({})", senderEmail, senderName);
            logger.info("  - To: {}", request.emailAddress());
            logger.info("  - Subject: {}", EMAIL_SUBJECT);
            logger.info("  - SES Region: {}", awsRegion);
            logger.info("  - AWS Authentication: Using IAM Role");
            
            // Generate HTML content
            String htmlContent = generateHtmlContent(request);
            logger.info("Generated HTML email content - length: {} characters", htmlContent.length());
            
            // Create SES send request
            SendEmailRequest sendRequest = createSendEmailRequest(request, htmlContent);
            
            logger.info("Email request created successfully. Attempting to send via AWS SES API...");
            
            // Send email via AWS SES API
            SendEmailResult result = sesClient.sendEmail(sendRequest);
            String messageId = result.getMessageId();
            
            long processingTime = System.currentTimeMillis() - startTime;
            logger.info("Email sent successfully to {} - SES Message ID: {} - Processing time: {}ms", 
                       request.emailAddress(), messageId, processingTime);
            
            return messageId;
            
        } catch (MessageRejectedException e) {
            logger.error("AWS SES rejected the message: {}", e.getMessage(), e);
            throw new EmailSendException("SES_MESSAGE_REJECTED", 
                "AWS SES rejected the message: " + e.getMessage(), e);
                
        } catch (MailFromDomainNotVerifiedException e) {
            logger.error("Sender email domain not verified in SES: {}", e.getMessage(), e);
            throw new EmailSendException("SES_SENDER_NOT_VERIFIED", 
                "Sender email domain not verified in AWS SES: " + e.getMessage(), e);
                
        } catch (ConfigurationSetDoesNotExistException e) {
            logger.error("SES configuration set does not exist: {}", e.getMessage(), e);
            throw new EmailSendException("SES_CONFIG_ERROR", 
                "SES configuration error: " + e.getMessage(), e);
                
        } catch (AmazonSimpleEmailServiceException e) {
            logger.error("AWS SES service error: {} (Error Code: {})", e.getMessage(), e.getErrorCode(), e);
            
            // Handle specific SES error codes
            String errorCode = e.getErrorCode();
            if ("Throttling".equals(errorCode) || "SendingPausedException".equals(errorCode)) {
                throw new EmailSendException("SES_RATE_LIMIT", 
                    "SES sending rate limit exceeded: " + e.getMessage(), e);
            } else if ("AccessDenied".equals(errorCode)) {
                throw new EmailSendException("SES_ACCESS_DENIED", 
                    "Access denied to SES. Check IAM permissions: " + e.getMessage(), e);
            } else {
                throw new EmailSendException("SES_SERVICE_ERROR", 
                    "AWS SES service error: " + e.getMessage(), e);
            }
            
        } catch (Exception e) {
            logger.error("Unexpected error during email sending: {}", e.getMessage(), e);
            throw new EmailSendException("INTERNAL_ERROR", 
                "Unexpected error during email sending: " + e.getMessage(), e);
        }
    }
    
    /**
     * Creates a SendEmailRequest for AWS SES API
     * 
     * @param request EmailRequest with recipient details
     * @param htmlContent Generated HTML email content
     * @return Configured SendEmailRequest
     */
    private SendEmailRequest createSendEmailRequest(EmailRequest request, String htmlContent) {
        // Create destination
        Destination destination = new Destination().withToAddresses(request.emailAddress());
        
        // Create message content
        Content subject = new Content().withData(EMAIL_SUBJECT).withCharset("UTF-8");
        Content htmlBody = new Content().withData(htmlContent).withCharset("UTF-8");
        
        // Create body with HTML content
        Body body = new Body().withHtml(htmlBody);
        
        // Create message
        Message message = new Message()
                .withSubject(subject)
                .withBody(body);
        
        // Create and return send request
        return new SendEmailRequest()
                .withSource(formatSenderAddress())
                .withDestination(destination)
                .withMessage(message);
    }
    
    /**
     * Formats the sender address with name and email
     * 
     * @return Formatted sender address
     */
    private String formatSenderAddress() {
        if (senderName != null && !senderName.trim().isEmpty()) {
            return String.format("%s <%s>", senderName, senderEmail);
        }
        return senderEmail;
    }

    /**
     * Validates email address format
     * 
     * @param email Email address to validate
     * @throws EmailSendException if email format is invalid
     */
    private void validateEmailAddress(String email) throws EmailSendException {
        if (email == null || email.trim().isEmpty()) {
            throw new EmailSendException("INVALID_EMAIL", 
                "Email address is required");
        }
        
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new EmailSendException("INVALID_EMAIL", 
                "Invalid email address format: " + email);
        }
        
        logger.debug("Email address validation passed: {}", email);
    }

    /**
     * Generates HTML content for the email
     * 
     * Creates a professional HTML email with:
     * - Responsive design
     * - Policy information in a formatted table
     * - Clean, readable styling
     * 
     * @param request EmailRequest with policy details
     * @return HTML string for email body
     */
    private String generateHtmlContent(EmailRequest request) {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='en'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Policy Information</title>");
        html.append("<style>");
        html.append("body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; ");
        html.append("line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; ");
        html.append("padding: 20px; background-color: #f5f5f5; }");
        html.append(".container { background-color: white; border-radius: 8px; ");
        html.append("box-shadow: 0 2px 4px rgba(0,0,0,0.1); overflow: hidden; }");
        html.append(".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); ");
        html.append("color: white; padding: 30px; text-align: center; }");
        html.append(".header h1 { margin: 0; font-size: 28px; font-weight: 300; }");
        html.append(".content { padding: 30px; }");
        html.append(".greeting { font-size: 18px; margin-bottom: 25px; color: #555; }");
        html.append("table { width: 100%; border-collapse: collapse; margin: 20px 0; }");
        html.append("th { background-color: #f8f9fa; padding: 12px; text-align: left; ");
        html.append("font-weight: 600; color: #667eea; border-bottom: 2px solid #dee2e6; }");
        html.append("td { padding: 12px; border-bottom: 1px solid #dee2e6; }");
        html.append("tr:last-child td { border-bottom: none; }");
        html.append(".label { font-weight: 500; color: #666; width: 40%; }");
        html.append(".value { color: #333; width: 60%; }");
        html.append(".footer { background-color: #f8f9fa; padding: 20px; ");
        html.append("text-align: center; color: #666; font-size: 14px; }");
        html.append(".footer p { margin: 5px 0; }");
        html.append("@media only screen and (max-width: 600px) {");
        html.append("body { padding: 10px; }");
        html.append(".header { padding: 20px; }");
        html.append(".header h1 { font-size: 24px; }");
        html.append(".content { padding: 20px; }");
        html.append("table { font-size: 14px; }");
        html.append("}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        
        // Header
        html.append("<div class='header'>");
        html.append("<h1>Policy Information</h1>");
        html.append("</div>");
        
        // Content
        html.append("<div class='content'>");
        
        // Greeting
        html.append("<div class='greeting'>");
        html.append("Dear ").append(escapeHtml(request.firstName())).append(" ");
        html.append(escapeHtml(request.lastName())).append(",");
        html.append("</div>");
        
        html.append("<p>Please find your policy information details below:</p>");
        
        // Policy Information Table
        html.append("<table>");
        html.append("<thead>");
        html.append("<tr><th colspan='2'>Policy Details</th></tr>");
        html.append("</thead>");
        html.append("<tbody>");
        
        // Customer Name
        html.append("<tr>");
        html.append("<td class='label'>Customer Name</td>");
        html.append("<td class='value'>").append(escapeHtml(request.firstName()))
                   .append(" ").append(escapeHtml(request.lastName())).append("</td>");
        html.append("</tr>");
        
        // Email Address
        html.append("<tr>");
        html.append("<td class='label'>Email Address</td>");
        html.append("<td class='value'>").append(escapeHtml(request.emailAddress())).append("</td>");
        html.append("</tr>");
        
        // Policy Number
        html.append("<tr>");
        html.append("<td class='label'>Policy Number</td>");
        html.append("<td class='value'>").append(escapeHtml(request.policyNumber())).append("</td>");
        html.append("</tr>");
        
        // VIN
        html.append("<tr>");
        html.append("<td class='label'>Vehicle Identification Number (VIN)</td>");
        html.append("<td class='value'>").append(escapeHtml(request.vin())).append("</td>");
        html.append("</tr>");
        
        // Date Generated
        html.append("<tr>");
        html.append("<td class='label'>Date Generated</td>");
        html.append("<td class='value'>").append(currentDate).append("</td>");
        html.append("</tr>");
        
        html.append("</tbody>");
        html.append("</table>");
        
        html.append("<p style='margin-top: 25px; color: #666;'>");
        html.append("If you have any questions about your policy, please contact our customer service team.</p>");
        
        html.append("</div>");
        
        // Footer
        html.append("<div class='footer'>");
        html.append("<p>This is an automated message from AI Agent System</p>");
        html.append("<p>Please do not reply to this email</p>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");
        
        logger.debug("Generated HTML email content - length: {} characters", html.length());
        
        return html.toString();
    }

    /**
     * Escapes HTML special characters to prevent XSS
     * 
     * @param text Text to escape
     * @return Escaped text safe for HTML
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /**
     * Custom exception for email sending errors
     */
    public static class EmailSendException extends Exception {
        private final String errorCode;
        
        public EmailSendException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        
        public EmailSendException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }
        
        public String getErrorCode() {
            return errorCode;
        }
    }
}