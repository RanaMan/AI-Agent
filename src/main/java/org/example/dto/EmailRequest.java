package org.example.dto;

/**
 * EmailRequest - Data Transfer Object for email sending requests
 * 
 * This record represents the JSON payload for sending policy information emails.
 * It contains customer and policy details that will be formatted into an HTML email.
 * 
 * Example JSON payload:
 * {
 *   "emailAddress": "customer@example.com",
 *   "firstName": "John",
 *   "lastName": "Doe",
 *   "policyNumber": "POL-123456",
 *   "vin": "1HGBH41JXMN109186"
 * }
 * 
 * Benefits of using a record:
 * - Immutable by default
 * - Automatic equals(), hashCode(), and toString() methods
 * - Compact syntax for data carriers
 * - Built-in null safety through constructor validation
 */
public record EmailRequest(
    /**
     * Recipient's email address
     * Expected format: valid email address (e.g., user@example.com)
     */
    String emailAddress,
    
    /**
     * Customer's first name
     * Used for personalization in the email content
     */
    String firstName,
    
    /**
     * Customer's last name
     * Used for personalization in the email content
     */
    String lastName,
    
    /**
     * Insurance policy number
     * Unique identifier for the customer's policy
     */
    String policyNumber,
    
    /**
     * Vehicle Identification Number
     * 17-character unique identifier for the insured vehicle
     */
    String vin
) {}