# Step 2B Testing Guide - Claude Function/Tool Definitions

## Overview
This guide outlines the testing scenarios for Step 2B implementation. The system now includes Claude function calling with mock tool execution.

## Testing Scenarios

### Test 1: PDF Analysis Request
**Endpoint:** `POST /api/chat/message`
**Request:**
```
FormData:
- message: "Can you analyze this PDF for VIN numbers?"
- files: [test.pdf]
- conversationId: "test-pdf-123"
```

**Expected Response:**
```json
{
  "status": "success",
  "response": "I'll help you analyze that PDF for VIN numbers! [MOCK EXECUTION] Would analyze PDF file '/uploaded/test.pdf' for: extract VIN numbers. This would extract text content, apply the analysis request, and return structured results. In Step 2C, this will call the actual PDF processor service.",
  "conversationId": "test-pdf-123",
  "timestamp": "2025-01-07T10:30:00Z",
  "processingTimeMs": 1200,
  "toolsUsed": ["analyze_pdf"],
  "error": null
}
```

**Validation Points:**
- ✅ `toolsUsed` contains `["analyze_pdf"]`
- ✅ Response mentions mock execution
- ✅ File path `/uploaded/test.pdf` is referenced
- ✅ Processing time logged

### Test 2: Email Request
**Endpoint:** `POST /api/chat/message`
**Request:**
```
FormData:
- message: "Send an email to john.doe@example.com with policy POL-12345 and VIN ABC123DEF456. Customer name is John Doe."
- conversationId: "test-email-456"
```

**Expected Response:**
```json
{
  "status": "success",
  "response": "I'll send that policy information email for you! [MOCK EXECUTION] Would send policy information email to john.doe@example.com (John Doe). Email would contain policy number POL-12345 and VIN ABC123DEF456 in a professionally formatted HTML template. In Step 2C, this will call the actual AWS SES email service.",
  "conversationId": "test-email-456",
  "timestamp": "2025-01-07T10:31:00Z",
  "processingTimeMs": 800,
  "toolsUsed": ["send_policy_email"],
  "error": null
}
```

**Validation Points:**
- ✅ `toolsUsed` contains `["send_policy_email"]`
- ✅ All email parameters extracted and mentioned
- ✅ Mock execution clearly indicated
- ✅ Professional explanation of what would happen

### Test 3: Image Analysis Request
**Endpoint:** `POST /api/chat/message`
**Request:**
```
FormData:
- message: "What can you see in this image?"
- files: [car_photo.jpg]
- conversationId: "test-image-789"
```

**Expected Response:**
```json
{
  "status": "success",
  "response": "Let me analyze that image for you! [MOCK EXECUTION] Would analyze image file '/uploaded/car_photo.jpg' for: general image description. This would process the visual content, identify objects/text/details, and return structured results. In Step 2C, this will call the actual image analysis service.",
  "conversationId": "test-image-789",
  "timestamp": "2025-01-07T10:32:00Z",
  "processingTimeMs": 950,
  "toolsUsed": ["analyze_image"],
  "error": null
}
```

**Validation Points:**
- ✅ `toolsUsed` contains `["analyze_image"]`
- ✅ Image file path correctly referenced
- ✅ Analysis type specified (general description)
- ✅ Mock execution explained

### Test 4: Regular Chat (No Tools)
**Endpoint:** `POST /api/chat/message`
**Request:**
```
FormData:
- message: "Hello, how are you today?"
- conversationId: "test-chat-101"
```

**Expected Response:**
```json
{
  "status": "success",
  "response": "Hello! I'm doing well, thank you for asking. I'm here to help you with document analysis, image analysis, and sending policy emails. Is there anything specific you'd like assistance with today?",
  "conversationId": "test-chat-101",
  "timestamp": "2025-01-07T10:33:00Z",
  "processingTimeMs": 600,
  "toolsUsed": [],
  "error": null
}
```

**Validation Points:**
- ✅ `toolsUsed` is empty array `[]`
- ✅ Natural conversational response
- ✅ No mock execution mentioned
- ✅ Capabilities mentioned naturally

### Test 5: Multiple Tools Request
**Endpoint:** `POST /api/chat/message`
**Request:**
```
FormData:
- message: "Please analyze this PDF document and then email the results to customer@example.com with policy POL-999 and VIN XYZ789. Customer name is Jane Smith."
- files: [report.pdf]
- conversationId: "test-multi-202"
```

**Expected Response:**
```json
{
  "status": "success",
  "response": "I'll analyze that PDF and then send the results via email! First, [MOCK EXECUTION] Would analyze PDF file '/uploaded/report.pdf' for: general document recap... Then, [MOCK EXECUTION] Would send policy information email to customer@example.com (Jane Smith). Email would contain policy number POL-999 and VIN XYZ789...",
  "conversationId": "test-multi-202",
  "timestamp": "2025-01-07T10:34:00Z",
  "processingTimeMs": 1500,
  "toolsUsed": ["analyze_pdf", "send_policy_email"],
  "error": null
}
```

**Validation Points:**
- ✅ `toolsUsed` contains both tools: `["analyze_pdf", "send_policy_email"]`
- ✅ Both mock executions explained
- ✅ Logical sequence of operations described
- ✅ All parameters for both tools mentioned

### Test 6: Conversation Memory with Tools
**Setup:** First message in conversation
**Request 1:**
```
FormData:
- message: "My name is Alex and I need help with document analysis"
- conversationId: "memory-test-303"
```

**Request 2:**
```
FormData:
- message: "Can you analyze this PDF for me?"
- files: [document.pdf]
- conversationId: "memory-test-303"
```

**Expected Response 2:**
```json
{
  "status": "success",  
  "response": "Of course, Alex! I'll analyze that PDF document for you. [MOCK EXECUTION] Would analyze PDF file '/uploaded/document.pdf' for: general document recap...",
  "conversationId": "memory-test-303",
  "timestamp": "2025-01-07T10:35:00Z",
  "processingTimeMs": 1100,
  "toolsUsed": ["analyze_pdf"],
  "error": null
}
```

**Validation Points:**
- ✅ Claude remembers the user's name (Alex) from previous message
- ✅ Tool calling works with conversation memory
- ✅ Context maintained across tool-enabled conversations

## Error Scenarios

### Test 7: API Key Invalid
**Setup:** Remove or invalidate `ANTHROPIC_API_KEY` environment variable
**Request:**
```
FormData:
- message: "Test message"
- conversationId: "error-test-401"
```

**Expected Response:**
```json
{
  "status": "error",
  "response": null,
  "conversationId": "error-test-401", 
  "timestamp": "2025-01-07T10:36:00Z",
  "processingTimeMs": 200,
  "toolsUsed": [],
  "error": {
    "code": "CLAUDE_API_KEY_INVALID",
    "message": "Invalid or expired API key",
    "timestamp": "2025-01-07T10:36:00Z"
  }
}
```

**Validation Points:**
- ✅ Proper error code returned
- ✅ Application doesn't crash
- ✅ Error structure maintained

## Logging Validation

During testing, verify these log messages appear:

### Service Initialization
```
INFO - ClaudeService initialized successfully with memory window size: 20
DEBUG - Chat agent initialized with system message and tools: Available tools: analyzePdf, analyzeImage, sendPolicyEmail
```

### Tool Usage Detection
```
INFO - Claude requested PDF analysis - file: /uploaded/test.pdf, prompt: 'extract VIN numbers'
DEBUG - Detected PDF analysis tool usage in response
INFO - Claude response generated successfully for conversation: test-123 - processing time: 1200ms, tools used: [analyze_pdf]
```

### Error Handling
```
WARN - Claude API call failed: CLAUDE_API_KEY_INVALID - Invalid or expired API key
```

## Success Criteria

Step 2B is successful when:

1. ✅ **Tool Definitions Working**: All 3 tools (PDF, image, email) are properly defined and accessible to Claude
2. ✅ **Function Call Detection**: System correctly identifies when Claude uses tools via mock execution markers
3. ✅ **Response Parsing**: `toolsUsed` field is accurately populated based on Claude's tool usage
4. ✅ **Mock Execution**: Clear indication of what Claude intends to do without actual execution
5. ✅ **Mixed Responses**: Claude can combine conversational text with tool usage explanations
6. ✅ **Error Handling**: Function calling errors are caught and handled gracefully
7. ✅ **Memory Preservation**: Conversation memory continues working with tool integration
8. ✅ **Backward Compatibility**: Regular chat (without tools) continues to work perfectly
9. ✅ **File Integration**: File uploads properly trigger tool usage when appropriate
10. ✅ **Logging**: Comprehensive logging of tool requests and mock executions

## Next Steps

After Step 2B validation:
- **Step 2C**: Replace mock execution with actual API calls to existing services
- **Step 2D**: Add response formatting and result integration  
- **Step 2E**: Complete end-to-end testing with real file processing

The foundation for intelligent tool selection and execution is now in place!