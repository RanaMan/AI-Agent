package org.example.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * PdfProcessorService - Service for processing PDF files and extracting text content
 * 
 * This service handles PDF text extraction using Apache PDFBox with comprehensive
 * error handling, text cleaning, and memory protection features.
 * 
 * Key features:
 * - Multi-page PDF text extraction
 * - Text cleaning and formatting
 * - Error handling for corrupted and password-protected PDFs
 * - Memory protection with character limits
 * - Comprehensive logging
 */
@Service
public class PdfProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(PdfProcessorService.class);
    
    // Constants for text processing
    private static final int MAX_TEXT_LENGTH = 50000; // 50K characters limit
    private static final Pattern EXCESSIVE_WHITESPACE = Pattern.compile("\\s{3,}");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\n{3,}");
    private static final Pattern TAB_PATTERN = Pattern.compile("\t+");

    /**
     * Extracts and processes text from a PDF file
     * 
     * @param file The PDF file to process
     * @return PdfProcessingResult containing extracted text and metadata
     * @throws PdfProcessingException if processing fails
     */
    public PdfProcessingResult extractTextFromPdf(MultipartFile file) throws PdfProcessingException {
        logger.info("Starting PDF text extraction for file: {}", file.getOriginalFilename());
        
        long startTime = System.currentTimeMillis();
        
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            
            // Check if PDF is password protected
            if (document.isEncrypted()) {
                logger.warn("PDF is password protected: {}", file.getOriginalFilename());
                throw new PdfProcessingException("PDF is password protected and cannot be processed");
            }
            
            // Get document metadata
            int pageCount = document.getNumberOfPages();
            logger.debug("PDF has {} pages", pageCount);
            
            // Extract text from all pages
            String rawText = extractTextFromAllPages(document);
            
            // Clean and format the extracted text
            String cleanedText = cleanAndFormatText(rawText);
            
            // Apply character limit for memory protection
            String finalText = applyCharacterLimit(cleanedText);
            
            long processingTime = System.currentTimeMillis() - startTime;
            
            PdfProcessingResult result = new PdfProcessingResult(
                finalText,
                pageCount,
                finalText.length(),
                rawText.length(),
                processingTime
            );
            
            logger.info("PDF text extraction completed successfully - pages: {}, final characters: {}, processing time: {}ms", 
                       pageCount, finalText.length(), processingTime);
            
            return result;
            
        } catch (IOException e) {
            logger.error("Failed to process PDF file: {}", file.getOriginalFilename(), e);
            
            // Determine specific error type
            String errorMessage = determineErrorMessage(e);
            throw new PdfProcessingException(errorMessage, e);
        }
    }

    /**
     * Extracts text from all pages of a PDF document
     * 
     * @param document The PDDocument to extract text from
     * @return Raw extracted text from all pages
     * @throws IOException if text extraction fails
     */
    private String extractTextFromAllPages(PDDocument document) throws IOException {
        PDFTextStripper textStripper = new PDFTextStripper();
        
        // Configure text stripper for better extraction
        textStripper.setSortByPosition(true);
        textStripper.setLineSeparator("\n");
        textStripper.setWordSeparator(" ");
        
        // Extract text from all pages (default behavior)
        textStripper.setStartPage(1);
        textStripper.setEndPage(document.getNumberOfPages());
        
        String extractedText = textStripper.getText(document);
        
        logger.debug("Raw text extracted - length: {} characters", extractedText.length());
        
        return extractedText;
    }

    /**
     * Cleans and formats the extracted text
     * 
     * Performs the following cleaning operations:
     * - Normalizes whitespace (removes excessive spaces)
     * - Reduces multiple newlines to maximum of 2
     * - Converts tabs to single spaces
     * - Trims leading and trailing whitespace
     * - Removes control characters
     * 
     * @param rawText The raw extracted text
     * @return Cleaned and formatted text
     */
    private String cleanAndFormatText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "";
        }
        
        logger.debug("Starting text cleaning - original length: {}", rawText.length());
        
        String cleanedText = rawText;
        
        // Remove or replace control characters (except newlines and tabs)
        cleanedText = cleanedText.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");
        
        // Convert tabs to single spaces
        cleanedText = TAB_PATTERN.matcher(cleanedText).replaceAll(" ");
        
        // Reduce excessive whitespace (3 or more spaces) to 2 spaces
        cleanedText = EXCESSIVE_WHITESPACE.matcher(cleanedText).replaceAll("  ");
        
        // Reduce multiple newlines (3 or more) to 2 newlines
        cleanedText = MULTIPLE_NEWLINES.matcher(cleanedText).replaceAll("\n\n");
        
        // Trim leading and trailing whitespace
        cleanedText = cleanedText.trim();
        
        logger.debug("Text cleaning completed - cleaned length: {}", cleanedText.length());
        
        return cleanedText;
    }

    /**
     * Applies character limit to prevent memory issues
     * 
     * @param text The text to limit
     * @return Text limited to MAX_TEXT_LENGTH characters
     */
    private String applyCharacterLimit(String text) {
        if (text.length() <= MAX_TEXT_LENGTH) {
            return text;
        }
        
        String limitedText = text.substring(0, MAX_TEXT_LENGTH);
        
        // Try to end at a word boundary to avoid cutting words in half
        int lastSpaceIndex = limitedText.lastIndexOf(' ');
        if (lastSpaceIndex > MAX_TEXT_LENGTH - 100) { // Only if space is reasonably close to limit
            limitedText = limitedText.substring(0, lastSpaceIndex);
        }
        
        logger.info("Text truncated from {} to {} characters due to length limit", 
                   text.length(), limitedText.length());
        
        return limitedText;
    }

    /**
     * Determines specific error message based on IOException
     * 
     * @param e The IOException that occurred
     * @return Appropriate error message
     */
    private String determineErrorMessage(IOException e) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("password") || message.contains("encrypted")) {
            return "PDF is password protected and cannot be processed";
        } else if (message.contains("corrupt") || message.contains("damaged")) {
            return "PDF file appears to be corrupted or damaged";
        } else if (message.contains("not a pdf") || message.contains("invalid pdf")) {
            return "File is not a valid PDF document";
        } else if (message.contains("memory") || message.contains("heap")) {
            return "PDF file is too large to process";
        } else {
            return "Failed to extract text from PDF: " + e.getMessage();
        }
    }

    /**
     * Result class containing PDF processing results and metadata
     */
    public static class PdfProcessingResult {
        private final String extractedText;
        private final int pageCount;
        private final int finalCharacterCount;
        private final int originalCharacterCount;
        private final long processingTimeMs;

        public PdfProcessingResult(String extractedText, int pageCount, int finalCharacterCount, 
                                 int originalCharacterCount, long processingTimeMs) {
            this.extractedText = extractedText;
            this.pageCount = pageCount;
            this.finalCharacterCount = finalCharacterCount;
            this.originalCharacterCount = originalCharacterCount;
            this.processingTimeMs = processingTimeMs;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public int getPageCount() {
            return pageCount;
        }

        public int getFinalCharacterCount() {
            return finalCharacterCount;
        }

        public int getOriginalCharacterCount() {
            return originalCharacterCount;
        }

        public long getProcessingTimeMs() {
            return processingTimeMs;
        }

        public boolean wasTruncated() {
            return originalCharacterCount > finalCharacterCount;
        }
    }

    /**
     * Custom exception for PDF processing errors
     */
    public static class PdfProcessingException extends Exception {
        public PdfProcessingException(String message) {
            super(message);
        }

        public PdfProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}