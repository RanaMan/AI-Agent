package org.example;

import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Investigation class to test LangChain4j 1.2.0 capabilities for tool execution tracking.
 * This class attempts to import and use various APIs to determine what's available.
 */
public class LangChain4jApiInvestigation {
    
    private static final Logger logger = LoggerFactory.getLogger(LangChain4jApiInvestigation.class);
    
    /**
     * Interface to test Result wrapper functionality
     */
    public interface TestAgent {
        
        // Test basic chat method
        String chat(String message);
    }
    
    public static void investigateAvailableApis() {
        logger.info("=== LangChain4j 1.2.0 API Investigation ===");
        
        // Test 1: Check if Result wrapper is available
        try {
            Class<?> resultClass = Class.forName("dev.langchain4j.service.Result");
            logger.info("✓ Result class is available: {}", resultClass.getName());
            
            // Check methods available on Result
            java.lang.reflect.Method[] methods = resultClass.getMethods();
            logger.info("Result class methods:");
            for (java.lang.reflect.Method method : methods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    logger.info("  - {} returns {}", method.getName(), method.getReturnType().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.warn("✗ Result class not available: {}", e.getMessage());
        }
        
        // Test 2: Check if ToolExecution is available
        try {
            Class<?> toolExecutionClass = Class.forName("dev.langchain4j.service.ToolExecution");
            logger.info("✓ ToolExecution class is available: {}", toolExecutionClass.getName());
            
            // Check methods available on ToolExecution
            java.lang.reflect.Method[] methods = toolExecutionClass.getMethods();
            logger.info("ToolExecution class methods:");
            for (java.lang.reflect.Method method : methods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    logger.info("  - {} returns {}", method.getName(), method.getReturnType().getSimpleName());
                }
            }
        } catch (Exception e) {
            logger.warn("✗ ToolExecution class not available: {}", e.getMessage());
        }
        
        // Test 3: Check if TokenStream is available and has onToolExecuted method
        try {
            Class<?> tokenStreamClass = Class.forName("dev.langchain4j.service.TokenStream");
            logger.info("✓ TokenStream class is available: {}", tokenStreamClass.getName());
            
            // Check methods available on TokenStream
            java.lang.reflect.Method[] methods = tokenStreamClass.getMethods();
            logger.info("TokenStream class methods (looking for tool execution callbacks):");
            for (java.lang.reflect.Method method : methods) {
                if (!method.getDeclaringClass().equals(Object.class) && 
                    method.getName().toLowerCase().contains("tool")) {
                    logger.info("  - {} returns {}", method.getName(), method.getReturnType().getSimpleName());
                    
                    // Check parameters
                    Class<?>[] paramTypes = method.getParameterTypes();
                    if (paramTypes.length > 0) {
                        logger.info("    Parameters: {}", java.util.Arrays.toString(paramTypes));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("✗ TokenStream class not available: {}", e.getMessage());
        }
        
        // Test 4: Check AiServices builder capabilities
        try {
            logger.info("✓ Checking AiServices builder capabilities...");
            
            // Get the builder class
            Object builder = AiServices.builder(TestAgent.class);
            Class<?> builderClass = builder.getClass();
            
            logger.info("AiServices builder class: {}", builderClass.getName());
            
            // Check methods available on the builder
            java.lang.reflect.Method[] methods = builderClass.getMethods();
            logger.info("AiServices builder methods (tool/streaming related):");
            for (java.lang.reflect.Method method : methods) {
                if (!method.getDeclaringClass().equals(Object.class)) {
                    String methodName = method.getName().toLowerCase();
                    if (methodName.contains("tool") || methodName.contains("stream") || 
                        methodName.contains("listener") || methodName.contains("callback")) {
                        logger.info("  - {} returns {}", method.getName(), method.getReturnType().getSimpleName());
                        
                        // Check parameters
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length > 0) {
                            logger.info("    Parameters: {}", java.util.Arrays.toString(paramTypes));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("✗ Error investigating AiServices builder: {}", e.getMessage());
        }
        
        logger.info("=== End Investigation ===");
    }
}