package org.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for serving the chatbot HTML page and configuration.
 * This controller provides endpoints for the chatbot interface and its configuration,
 * allowing the Copilot Studio URL to be configured via application properties.
 */
@Controller
public class ChatbotController {

    @Value("${chatbot.copilot.studio.url}")
    private String copilotStudioUrl;

    /**
     * Serves the chatbot page at the root URL.
     * 
     * @return Forward to the static chatbot.html file
     */
    @GetMapping("/")
    public String rootChatbot() {
        return "forward:/chatbot.html";
    }

    /**
     * Serves the chatbot page at /chatbot URL.
     * 
     * @return Forward to the static chatbot.html file
     */
    @GetMapping("/chatbot")
    public String chatbot() {
        return "forward:/chatbot.html";
    }

    /**
     * Provides the chatbot configuration as JSON.
     * This endpoint is called by the chatbot.html JavaScript to get the 
     * Copilot Studio URL from configuration.
     * 
     * @return Map containing the Copilot Studio URL
     */
    @GetMapping("/api/chatbot-config")
    @ResponseBody
    public Map<String, String> getChatbotConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("copilotStudioUrl", copilotStudioUrl);
        return config;
    }
}