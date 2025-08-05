# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview
LangChain4j AI Agent demonstration using Anthropic's Claude AI as a technical consultant with memory capabilities.

## Build Commands
- `mvn clean install`: Install dependencies and build
- `mvn exec:java -Dexec.mainClass="org.example.Main"`: Run the application
- `mvn clean package`: Build JAR for deployment
- `mvn test`: Run tests (no tests currently implemented)

## Architecture
The codebase follows a simple but extensible pattern for AI agent creation:

1. **Main.java**: Entry point that configures Claude AI model with API key, model name, temperature, and max tokens
2. **TechnicalConsultantAgent.java**: Interface defining agent capabilities using LangChain4j annotations (@SystemMessage, @UserMessage, @V)
3. **AgentExample.java**: Implementation that creates the agent with MessageWindowChatMemory (20 message history) and demonstrates usage

Key architectural decisions:
- Uses interface-based agent definition for flexibility
- Memory management via MessageWindowChatMemory for contextual conversations
- Builder pattern for AI model and service configuration

## Environment Setup
- **Required**: Set `ANTHROPIC_API_KEY` environment variable before running
- Java version: 24 (as specified in pom.xml)
- LangChain4j version: 1.1.0

## Security Considerations
- API keys must use environment variables (never hardcode)
- .gitignore configured to exclude .env files and secrets

## Deployment
- Platform: AWS Elastic Beanstalk (Java SE, t3.micro)
- CI/CD: GitHub Actions with OIDC authentication
- Packaging: Maven Shade plugin creates fat JAR as `application.jar`
- Configuration: `.ebextensions/` for EB customization
- See DEPLOYMENT.md for detailed AWS setup instructions2