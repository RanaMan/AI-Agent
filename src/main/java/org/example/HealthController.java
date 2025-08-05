package org.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health check REST controller for monitoring application status.
 * This controller provides a simple endpoint that can be used by load balancers,
 * monitoring tools, or deployment platforms (like AWS Elastic Beanstalk) to verify
 * that the application is running properly.
 * 
 * @RestController combines @Controller and @ResponseBody, meaning all methods
 * return data directly (as JSON by default) rather than view names.
 */
@RestController
public class HealthController {
    
    /**
     * Health check endpoint that returns the current status of the application.
     * 
     * @GetMapping maps HTTP GET requests to /health to this method.
     * The method returns a Map which Spring automatically converts to JSON.
     * 
     * @return A map containing status information and current timestamp
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "AI Agent Application");
        response.put("timestamp", Instant.now().toString());
        response.put("message", "Service is running normally");
        
        return response;
    }
}