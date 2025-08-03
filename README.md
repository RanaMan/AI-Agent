# AI-Agent

A Java application demonstrating the use of LangChain4j with Anthropic's Claude AI to create an intelligent technical consultant agent.

## Overview

This project showcases how to:
- Integrate Claude AI into a Java application using LangChain4j
- Create an AI agent with memory capabilities for context-aware conversations
- Build a technical consultant that can analyze problems and review code
- Implement proper AI agent patterns using interfaces and annotations

## Project Structure

```
AI_Agent/
├── src/main/java/org/example/
│   ├── Main.java                      # Application entry point
│   ├── AgentExample.java              # Agent demonstration and usage examples
│   └── TechnicalConsultantAgent.java  # AI agent interface definition
├── pom.xml                            # Maven dependencies
└── .gitignore                         # Git ignore rules
```

## Prerequisites

- Java 11 or higher
- Maven
- An Anthropic API key (get one at https://console.anthropic.com/)

## Setup

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/AI-Agent.git
cd AI-Agent
```

2. Set your Anthropic API key as an environment variable:

**Windows (Command Prompt):**
```cmd
set ANTHROPIC_API_KEY=your-api-key-here
```

**Windows (PowerShell):**
```powershell
$env:ANTHROPIC_API_KEY="your-api-key-here"
```

**Linux/Mac:**
```bash
export ANTHROPIC_API_KEY="your-api-key-here"
```

3. Install dependencies:
```bash
mvn clean install
```

4. Run the application:
```bash
mvn exec:java -Dexec.mainClass="org.example.Main"
```

## Features

### Technical Consultant Agent
The agent can:
- **Analyze Problems**: Provide detailed analysis of technical issues
- **Review Code**: Offer code review with suggestions for improvements
- **Maintain Context**: Remember previous conversations for follow-up questions

### Example Use Cases
1. **Problem Analysis**: Diagnose Spring Boot performance issues
2. **Code Review**: Review REST controllers for best practices
3. **Technical Consultation**: Recommend monitoring tools and solutions

## Security Note

**NEVER commit your API key to version control!** Always use environment variables or secure configuration management for sensitive credentials.

## Technologies Used

- **Java**: Core programming language
- **LangChain4j**: Framework for building AI applications
- **Anthropic Claude AI**: Large language model for intelligent responses
- **Maven**: Dependency management

## How It Works

1. **Main.java**: Initializes the Claude AI model with configuration
2. **TechnicalConsultantAgent.java**: Defines the agent interface with AI annotations
3. **AgentExample.java**: Creates the agent with memory and demonstrates usage

The application uses LangChain4j's `AiServices` to automatically implement the agent interface, connecting it to Claude AI with conversation memory.

## Contributing

Feel free to fork this repository and submit pull requests for improvements.

## License

This project is for educational purposes. Please ensure you comply with Anthropic's terms of service when using their API.