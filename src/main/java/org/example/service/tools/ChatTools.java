package org.example.service.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import org.example.dto.EmailRequest;
import org.example.service.EmailService;
import org.example.service.PdfProcessorService;
import org.example.TechnicalConsultantAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;        // Add this import
import java.util.ArrayList;

/**
 * ChatTools - Tool definitions for Claude AI function calling
 * 
 * This class defines the available tools/functions that Claude can call
 * during chat conversations. In Step 2B, these tools perform mock execution
 * to demonstrate Claude's intent. Step 2C will implement actual execution.
 * 
 * Available tools:
 * - analyze_pdf: PDF document analysis and information extraction
 * - analyze_image: Image analysis and visual content examination
 * - send_policy_email: Policy information email sending
 */
@Component
public class ChatTools {

    private static final Logger logger = LoggerFactory.getLogger(ChatTools.class);
    
    @Autowired
    private PdfProcessorService pdfProcessorService;
    
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private TechnicalConsultantAgent technicalConsultantAgent;
    
    @Autowired
    private AnthropicChatModel claudeModel;
    
    // File cache for conversation-scoped file access
    private Map<String, MultipartFile> fileCache = new ConcurrentHashMap<>();
    
    // Tool execution tracking for current conversation
    private final ThreadLocal<List<String>> executedTools = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue() {
            return new ArrayList<>();
        }
    };

    /**
     * Tool for analyzing PDF documents and extracting information
     * 
     * This tool can analyze PDF files and extract specific information based on
     * user requests like VIN numbers, contact information, financial data, etc.
     * 
     * @param filePath Path to the PDF file (provided by file upload)
     * @param prompt Optional custom prompt for specific analysis (default: general recap)
     * @return Mock response indicating what analysis would be performed
     */
    @Tool("Analyzes PDF documents and extracts information or answers specific questions about the content")
    public String analyzePdf(String filePath, String prompt) {
        logger.info("Processing PDF analysis request - file: {}, prompt: '{}'", filePath, prompt);
        
        // Track tool execution
        executedTools.get().add("analyze_pdf");
        
        try {
            // Get the actual file from cache
            MultipartFile pdfFile = getFileFromPath(filePath);
            
            // Extract text using existing PDF processor service
            PdfProcessorService.PdfProcessingResult processingResult = 
                pdfProcessorService.extractTextFromPdf(pdfFile);
                
            // Use TechnicalConsultantAgent for AI-powered analysis
            String analysis;
            if (prompt != null && !prompt.trim().isEmpty()) {
                logger.debug("Using custom prompt for PDF analysis: {}", prompt);
                analysis = technicalConsultantAgent.analyzeWithCustomPrompt(prompt, 
                    processingResult.getExtractedText());
            } else {
                logger.debug("Using standard recap for PDF analysis");
                analysis = technicalConsultantAgent.recapDocument(processingResult.getExtractedText());
            }
            
            // Format the complete response with analysis and metadata
            String response = String.format(
                "PDF Analysis Results:\n%s\n\n" +
                "Document Details:\n" +
                "- File: %s\n" +
                "- Pages: %d\n" +
                "- Characters extracted: %d\n" +
                "- Analysis completed successfully",
                analysis,
                pdfFile.getOriginalFilename(),
                processingResult.getPageCount(),
                processingResult.getFinalCharacterCount()
            );
            
            logger.info("PDF analysis completed successfully - file: {}, pages: {}, chars: {}", 
                       pdfFile.getOriginalFilename(), processingResult.getPageCount(), 
                       processingResult.getFinalCharacterCount());
            
            return response;
            
        } catch (FileAccessException e) {
            logger.error("File access failed for PDF analysis: {}", filePath, e);
            return String.format("Sorry, I couldn't access the PDF file at '%s'. Please ensure the file was uploaded correctly.", filePath);
            
        } catch (PdfProcessorService.PdfProcessingException e) {
            logger.error("PDF processing failed for file: {}", filePath, e);
            return String.format("Sorry, I encountered an error processing the PDF: %s. The file might be corrupted, password-protected, or in an unsupported format.", e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during PDF analysis: {}", filePath, e);
            return String.format("Sorry, I encountered an unexpected error analyzing the PDF: %s", e.getMessage());
        }
    }
    


    /**
     * Tool for analyzing images and extracting visual information
     * 
     * This tool can analyze images and extract information like text, objects,
     * VIN numbers, damage assessment, or answer specific questions about visual content.
     * 
     * @param filePath Path to the image file (provided by file upload)
     * @param prompt Optional custom prompt for specific analysis (default: general description)
     * @return Mock response indicating what image analysis would be performed
     */
    @Tool("Analyzes images and extracts information or answers specific questions about visual content")
    public String analyzeImage(String filePath, String prompt) {
        logger.info("Processing image analysis request - file: {}, prompt: '{}'", filePath, prompt);
        
        // Track tool execution
        executedTools.get().add("analyze_image");
        
        try {
            // Get the actual file from cache
            MultipartFile imageFile = getFileFromPath(filePath);
            
            // Get image bytes and MIME type
            byte[] imageBytes = imageFile.getBytes();
            String mimeType = imageFile.getContentType();
            
            // Create UserMessage with both text and image content
            UserMessage visionMessage = UserMessage.from(
                TextContent.from(prompt != null && !prompt.trim().isEmpty() 
                    ? prompt 
                    : "Please describe what you see in this image in detail."),
                ImageContent.from(Base64.getEncoder().encodeToString(imageBytes), mimeType)
            );
            
            // Call Claude directly with the vision message
            String analysis = claudeModel.chat(visionMessage).aiMessage().text();
            
            // Format the complete response with analysis and metadata
            String analysisResponse = String.format(
                "Image Analysis Results:\n%s\n\n" +
                "Image Details:\n" +
                "- File: %s\n" +
                "- Size: %d bytes\n" +
                "- Type: %s\n" +
                "- Analysis completed successfully",
                analysis,
                imageFile.getOriginalFilename(),
                imageFile.getSize(),
                imageFile.getContentType()
            );
            
            logger.info("Image analysis completed successfully - file: {}, size: {} bytes", 
                       imageFile.getOriginalFilename(), imageFile.getSize());
            
            return analysisResponse;
            
        } catch (FileAccessException e) {
            logger.error("File access failed for image analysis: {}", filePath, e);
            return String.format("Sorry, I couldn't access the image file at '%s'. Please ensure the file was uploaded correctly.", filePath);
            
        } catch (IOException e) {
            logger.error("Failed to read image bytes for analysis: {}", filePath, e);
            return String.format("Sorry, I couldn't read the image file data: %s", e.getMessage());
            
        } catch (Exception e) {
            logger.error("Unexpected error during image analysis: {}", filePath, e);
            return String.format("Sorry, I encountered an error analyzing the image: %s", e.getMessage());
        }
    }
    

    /**
     * Tool for sending policy information emails to customers
     * 
     * This tool sends formatted HTML emails containing customer policy information
     * via AWS SES integration. All parameters are required for proper email delivery.
     * 
     * @param emailAddress Recipient's email address
     * @param firstName Customer's first name
     * @param lastName Customer's last name
     * @param policyNumber Insurance policy number
     * @param vin Vehicle identification number
     * @return Mock response indicating what email would be sent
     */
    @Tool("Sends policy information emails to customers with their policy and vehicle details")
    public String sendPolicyEmail(String emailAddress, String firstName, String lastName, 
                                  String policyNumber, String vin) {
        
        logger.info("Processing policy email send request - recipient: {}, policy: {}, VIN: {}", 
                   emailAddress, policyNumber, vin);
        
        // Track tool execution
        executedTools.get().add("send_policy_email");
        
        try {
            // Create EmailRequest DTO using the existing record structure
            EmailRequest emailRequest = new EmailRequest(emailAddress, firstName, lastName, policyNumber, vin);
            
            // Send email using existing AWS SES service
            String messageId = emailService.sendPolicyEmail(emailRequest);
            
            // Format successful response with details
            String response = String.format(
                "Policy information email sent successfully!\n\n" +
                "Email Details:\n" +
                "- Recipient: %s (%s %s)\n" +
                "- Policy Number: %s\n" +
                "- VIN: %s\n" +
                "- AWS SES Message ID: %s\n" +
                "- Email delivery confirmed",
                emailAddress, firstName, lastName, policyNumber, vin, messageId
            );
            
            logger.info("Policy email sent successfully - recipient: {}, messageId: {}", emailAddress, messageId);
            return response;
            
        } catch (EmailService.EmailSendException e) {
            logger.error("Email sending failed - recipient: {}, error: {}", emailAddress, e.getMessage(), e);
            
            // Provide user-friendly error message based on error type
            String userMessage = switch (e.getErrorCode()) {
                case "INVALID_EMAIL" -> 
                    String.format("Sorry, the email address '%s' appears to be invalid. Please check the email address and try again.", emailAddress);
                case "SES_SEND_FAILED" -> 
                    String.format("Sorry, I couldn't send the email to %s due to a delivery issue. This might be because the email address is not verified in our system.", emailAddress);
                case "AUTHENTICATION_FAILED" -> 
                    "Sorry, there was an authentication issue with our email service. Please try again later or contact support.";
                default -> 
                    String.format("Sorry, I encountered an error sending the email to %s: %s", emailAddress, e.getMessage());
            };
            
            return userMessage;
            
        } catch (Exception e) {
            logger.error("Unexpected error sending policy email to: {}", emailAddress, e);
            return String.format("Sorry, I encountered an unexpected error while sending the email to %s. Please try again later.", emailAddress);
        }
    }
    
    /**
     * Sets the file cache for this conversation (called from ClaudeService)
     * 
     * @param fileCache Map of file paths to MultipartFile objects
     */
    public void setFileCache(Map<String, MultipartFile> fileCache) {
        this.fileCache = fileCache;
    }
    
    /**
     * Gets the list of tools executed in the current thread/conversation
     * 
     * @return List of tool names that were executed
     */
    public List<String> getExecutedTools() {
        return new ArrayList<>(executedTools.get());
    }
    
    /**
     * Clears the executed tools list for the current thread (called at start of new requests)
     */
    public void clearExecutedTools() {
        executedTools.get().clear();
    }
    
    /**
     * Retrieves a file from the cache using the standardized file path
     * 
     * @param filePath The standardized file path (format: /uploaded/{conversationId}/{filename})
     * @return MultipartFile object for the specified path
     * @throws FileAccessException if file is not found in cache
     */
    private MultipartFile getFileFromPath(String filePath) throws FileAccessException {
        MultipartFile file = fileCache.get(filePath);
        if (file == null) {
            throw new FileAccessException("File not found in cache: " + filePath);
        }
        logger.debug("Retrieved file from cache: {} (size: {} bytes)", filePath, file.getSize());
        return file;
    }
    
    /**
     * Gets a summary of all available tools for debugging/monitoring
     * 
     * @return String describing all available tools
     */
    public String getAvailableToolsSummary() {
        return "Available tools: analyzePdf (PDF document analysis), analyzeImage (image analysis), " +
               "sendPolicyEmail (policy information emails)";
    }
    
    /**
     * Custom exception for file access errors
     */
    public static class FileAccessException extends Exception {
        public FileAccessException(String message) {
            super(message);
        }
        
        public FileAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}