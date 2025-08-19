package org.example.config;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * MailConfig - Configuration for AWS SES email sending using IAM roles
 * 
 * This configuration class sets up JavaMailSender to use AWS SES with
 * IAM role authentication instead of explicit access keys.
 */
@Configuration
public class MailConfig {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * Creates a JavaMailSender bean configured for AWS SES with IAM role authentication
     * 
     * @return Configured JavaMailSender instance
     */
    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // Set SMTP configuration for AWS SES
        mailSender.setHost("email-smtp." + awsRegion + ".amazonaws.com");
        mailSender.setPort(587);
        
        // Get AWS credentials from IAM role
        DefaultAWSCredentialsProviderChain credentialsProvider = DefaultAWSCredentialsProviderChain.getInstance();
        try {
            com.amazonaws.auth.AWSCredentials credentials = credentialsProvider.getCredentials();
            mailSender.setUsername(credentials.getAWSAccessKeyId());
            mailSender.setPassword(credentials.getAWSSecretKey());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve AWS credentials from IAM role", e);
        }
        
        // Configure mail properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
        props.put("mail.debug", "false");
        
        return mailSender;
    }

    /**
     * Creates an AmazonSimpleEmailService client using IAM role authentication
     * This can be used for additional SES operations beyond SMTP
     * 
     * @return Configured AmazonSimpleEmailService client
     */
    @Bean
    public AmazonSimpleEmailService amazonSimpleEmailService() {
        return AmazonSimpleEmailServiceClientBuilder.standard()
                .withRegion(Regions.fromName(awsRegion))
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .build();
    }
}