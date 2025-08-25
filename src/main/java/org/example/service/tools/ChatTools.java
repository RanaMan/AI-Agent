package org.example.service.tools;

import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
        logger.info("Claude requested PDF analysis - file: {}, prompt: '{}'", filePath, prompt);
        
        String analysisType = (prompt != null && !prompt.trim().isEmpty()) ? prompt : "general document recap";
        
        String mockResponse = String.format(
            "[MOCK EXECUTION] Would analyze PDF file '%s' for: %s. " +
            "This would extract text content, apply the analysis request, and return structured results. " +
            "In Step 2C, this will call the actual PDF processor service.",
            filePath, analysisType
        );
        
        logger.info("Mock PDF analysis completed for file: {}", filePath);
        return mockResponse;
    }
    
    /**
     * Tool for analyzing PDF documents with just the file path (overload for simpler calls)
     * 
     * @param filePath Path to the PDF file
     * @return Mock response indicating what analysis would be performed
     */
    @Tool("Analyzes PDF documents with general recap")
    public String analyzePdf(String filePath) {
        return analyzePdf(filePath, null);
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
        logger.info("Claude requested image analysis - file: {}, prompt: '{}'", filePath, prompt);
        
        String analysisType = (prompt != null && !prompt.trim().isEmpty()) ? prompt : "general image description";
        
        String mockResponse = String.format(
            "[MOCK EXECUTION] Would analyze image file '%s' for: %s. " +
            "This would process the visual content, identify objects/text/details, and return structured results. " +
            "In Step 2C, this will call the actual image analysis service.",
            filePath, analysisType
        );
        
        logger.info("Mock image analysis completed for file: {}", filePath);
        return mockResponse;
    }
    
    /**
     * Tool for analyzing images with just the file path (overload for simpler calls)
     * 
     * @param filePath Path to the image file
     * @return Mock response indicating what analysis would be performed
     */
    @Tool("Analyzes images with general description")
    public String analyzeImage(String filePath) {
        return analyzeImage(filePath, null);
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
        
        logger.info("Claude requested policy email - recipient: {}, policy: {}, VIN: {}", 
                   emailAddress, policyNumber, vin);
        
        String mockResponse = String.format(
            "[MOCK EXECUTION] Would send policy information email to %s (%s %s). " +
            "Email would contain policy number %s and VIN %s in a professionally formatted HTML template. " +
            "In Step 2C, this will call the actual AWS SES email service.",
            emailAddress, firstName, lastName, policyNumber, vin
        );
        
        logger.info("Mock policy email prepared for: {}", emailAddress);
        return mockResponse;
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
}