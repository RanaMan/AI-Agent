package org.example;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.dto.PdfRecapResponse;
import org.example.service.PdfProcessorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

/**
 * PdfProcessorController - REST controller for PDF document processing and recap generation
 * 
 * This controller provides endpoints for uploading PDF files, extracting text content,
 * and generating AI-powered recaps using Claude through the TechnicalConsultantAgent.
 * 
 * Key features:
 * - PDF file validation (MIME type, extension, file size)
 * - Text extraction using Apache PDFBox
 * - AI-powered document summarization
 * - Comprehensive error handling
 * - Structured JSON responses
 */
@RestController
@RequestMapping("/api/pdf")
@Tag(name = "PDF Processing", description = "API for uploading and processing PDF documents with AI-powered recap generation")
public class PdfProcessorController {

    private static final Logger logger = LoggerFactory.getLogger(PdfProcessorController.class);
    
    // Constants for validation
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB in bytes
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String PDF_EXTENSION = ".pdf";

    @Autowired
    private TechnicalConsultantAgent technicalConsultantAgent;
    
    @Autowired
    private PdfProcessorService pdfProcessorService;

    /**
     * POST endpoint for PDF recap generation
     * 
     * Accepts a PDF file upload, validates it, extracts text content,
     * and generates an AI-powered recap using Claude.
     * 
     * @param file The uploaded PDF file as MultipartFile
     * @return ResponseEntity with success/error JSON response
     */
    @PostMapping(value = "/recap", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Generate AI-powered PDF recap",
        description = "Upload a PDF file to extract text content and generate a structured AI-powered recap with executive summary, key points, main topics, and conclusion."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF processed successfully and recap generated",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PdfRecapResponse.class),
                examples = @ExampleObject(
                    name = "Success Response",
                    value = """
                    {
                      "status": "success",
                      "filename": "document.pdf",
                      "fileSize": 245632,
                      "pageCount": 5,
                      "recap": "Executive Summary: This document outlines the company's quarterly financial performance...\n\nKey Points:\n• Revenue increased by 15% compared to last quarter\n• Operating expenses remained stable\n• New product launch exceeded expectations\n\nMain Topics:\n- Financial Performance\n- Market Analysis\n- Strategic Initiatives\n\nConclusion: The company demonstrates strong growth trajectory with promising market positioning.",
                      "extractedCharacters": 15000,
                      "timestamp": "2025-01-07T10:30:00Z",
                      "processingTimeMs": 3500,
                      "error": null
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid file upload - wrong format, extension, or missing file",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PdfRecapResponse.class),
                examples = @ExampleObject(
                    name = "Invalid File Error",
                    value = """
                    {
                      "status": "error",
                      "filename": "document.txt",
                      "fileSize": 1024,
                      "pageCount": null,
                      "recap": null,
                      "extractedCharacters": null,
                      "timestamp": "2025-01-07T10:30:00Z",
                      "processingTimeMs": null,
                      "error": {
                        "code": "INVALID_FILE",
                        "message": "Invalid file type. Expected: application/pdf, but got: text/plain",
                        "timestamp": "2025-01-07T10:30:00Z"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "413",
            description = "File size exceeds maximum allowed limit (10MB)",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PdfRecapResponse.class),
                examples = @ExampleObject(
                    name = "File Size Exceeded Error",
                    value = """
                    {
                      "status": "error",
                      "filename": "large-document.pdf",
                      "fileSize": 12582912,
                      "pageCount": null,
                      "recap": null,
                      "extractedCharacters": null,
                      "timestamp": "2025-01-07T10:30:00Z",
                      "processingTimeMs": null,
                      "error": {
                        "code": "FILE_SIZE_EXCEEDED",
                        "message": "File size (12582912 bytes) exceeds maximum allowed size (10485760 bytes)",
                        "timestamp": "2025-01-07T10:30:00Z"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "422",
            description = "PDF processing failed - corrupted file, password protected, or extraction error",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PdfRecapResponse.class),
                examples = @ExampleObject(
                    name = "PDF Extraction Failed Error",
                    value = """
                    {
                      "status": "error",
                      "filename": "protected-document.pdf",
                      "fileSize": 500000,
                      "pageCount": null,
                      "recap": null,
                      "extractedCharacters": null,
                      "timestamp": "2025-01-07T10:30:00Z",
                      "processingTimeMs": null,
                      "error": {
                        "code": "PDF_EXTRACTION_FAILED",
                        "message": "PDF is password protected and cannot be processed",
                        "timestamp": "2025-01-07T10:30:00Z"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error - unexpected system failure",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = PdfRecapResponse.class),
                examples = @ExampleObject(
                    name = "Internal Error",
                    value = """
                    {
                      "status": "error",
                      "filename": null,
                      "fileSize": null,
                      "pageCount": null,
                      "recap": null,
                      "extractedCharacters": null,
                      "timestamp": "2025-01-07T10:30:00Z",
                      "processingTimeMs": null,
                      "error": {
                        "code": "INTERNAL_ERROR",
                        "message": "An unexpected error occurred while processing the PDF",
                        "timestamp": "2025-01-07T10:30:00Z"
                      }
                    }
                    """
                )
            )
        )
    })
    public ResponseEntity<PdfRecapResponse> recapPdf(
            @Parameter(
                description = "PDF file to process (max 10MB, .pdf extension required)",
                required = true,
                content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestParam("file") MultipartFile file) {
        
        long startTime = System.currentTimeMillis();
        String timestamp = Instant.now().toString();
        
        logger.info("PDF recap request received - filename: {}, size: {} bytes", 
                   file.getOriginalFilename(), file.getSize());

        try {
            // Validate the uploaded file
            validatePdfFile(file);
            
            // Extract and process text from PDF using service
            PdfProcessorService.PdfProcessingResult processingResult = pdfProcessorService.extractTextFromPdf(file);
            
            // Generate recap using Claude AI
            String recap = technicalConsultantAgent.recapDocument(processingResult.getExtractedText());
            
            // Calculate total processing time (including service processing time)
            long totalProcessingTime = System.currentTimeMillis() - startTime;
            
            // Build success response using DTO
            PdfRecapResponse response = PdfRecapResponse.success(
                file.getOriginalFilename(),
                file.getSize(),
                processingResult.getPageCount(),
                recap,
                processingResult.getFinalCharacterCount(),
                timestamp,
                totalProcessingTime
            );
            
            logger.info("PDF recap completed successfully - filename: {}, processing time: {}ms", 
                       file.getOriginalFilename(), totalProcessingTime);
            
            return ResponseEntity.ok(response);
            
        } catch (InvalidFileException e) {
            logger.warn("Invalid file upload: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                PdfRecapResponse.errorWithMetadata(
                    file.getOriginalFilename(),
                    file.getSize(),
                    "INVALID_FILE",
                    e.getMessage(),
                    timestamp
                )
            );
            
        } catch (FileSizeExceededException e) {
            logger.warn("File size exceeded: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(
                PdfRecapResponse.errorWithMetadata(
                    file.getOriginalFilename(),
                    file.getSize(),
                    "FILE_SIZE_EXCEEDED",
                    e.getMessage(),
                    timestamp
                )
            );
            
        } catch (PdfProcessorService.PdfProcessingException e) {
            logger.error("PDF processing failed: {}", e.getMessage(), e);
            return ResponseEntity.unprocessableEntity().body(
                PdfRecapResponse.errorWithMetadata(
                    file.getOriginalFilename(),
                    file.getSize(),
                    "PDF_EXTRACTION_FAILED",
                    e.getMessage(),
                    timestamp
                )
            );
            
        } catch (Exception e) {
            logger.error("Unexpected error during PDF processing: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                PdfRecapResponse.error(
                    "INTERNAL_ERROR",
                    "An unexpected error occurred while processing the PDF",
                    timestamp
                )
            );
        }
    }

    /**
     * Validates the uploaded PDF file
     * 
     * Checks:
     * - File is not null or empty
     * - File size is within limits
     * - File has correct MIME type
     * - File has correct extension
     * 
     * @param file The uploaded file to validate
     * @throws InvalidFileException if validation fails
     * @throws FileSizeExceededException if file size exceeds limit
     */
    private void validatePdfFile(MultipartFile file) throws InvalidFileException, FileSizeExceededException {
        // Check if file is null or empty
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file uploaded or file is empty");
        }
        
        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileSizeExceededException(
                String.format("File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                             file.getSize(), MAX_FILE_SIZE));
        }
        
        // Check MIME type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(PDF_MIME_TYPE)) {
            throw new InvalidFileException(
                String.format("Invalid file type. Expected: %s, but got: %s", 
                             PDF_MIME_TYPE, contentType));
        }
        
        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(PDF_EXTENSION)) {
            throw new InvalidFileException(
                String.format("Invalid file extension. Expected: %s", PDF_EXTENSION));
        }
        
        logger.debug("PDF file validation passed - filename: {}, size: {} bytes", filename, file.getSize());
    }



    // Custom exception classes for better error handling
    
    /**
     * Exception thrown when uploaded file is invalid
     */
    public static class InvalidFileException extends Exception {
        public InvalidFileException(String message) {
            super(message);
        }
    }

    /**
     * Exception thrown when file size exceeds limits
     */
    public static class FileSizeExceededException extends Exception {
        public FileSizeExceededException(String message) {
            super(message);
        }
    }

}