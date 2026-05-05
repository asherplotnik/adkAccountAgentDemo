package com.idb.directchannels.adkAccountAgentDemo.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoUiController {

    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${agent.base-url:}")
    private String configuredAgentBaseUrl;

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "healthy", "app", "adkAccountAgentDemo");
    }

    @GetMapping("/.well-known/agent.json")
    public Map<String, Object> agentCard() {
        String baseUrl = configuredAgentBaseUrl == null || configuredAgentBaseUrl.isBlank()
                ? "http://localhost:" + serverPort
                : configuredAgentBaseUrl;

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("name", "Current Account Platform Specialist");
        card.put("description", "A specialized read-only A2A agent for current-account summary and filtered transactions.");
        card.put("version", "1.0.0");
        card.put("protocol", "a2a");
        card.put("protocolVersion", "1.0");
        card.put("provider", Map.of("name", "IDB Direct Channels", "url", baseUrl));
        card.put("capabilities", Map.of("streaming", false, "pushNotifications", false, "stateTransitionHistory", false));
        card.put("authentication", Map.of("schemes", List.of("bearer")));
        card.put("defaultInputModes", List.of("text"));
        card.put("defaultOutputModes", List.of("text"));
        card.put("skills", List.of(
                Map.of(
                        "id", "current-account-summary",
                        "name", "Current Account Summary",
                        "tags", List.of("banking", "current-account", "summary"),
                        "examples", List.of(
                                "What is my available balance?",
                                "Show both balance and available balance")),
                Map.of(
                        "id", "filtered-transactions",
                        "name", "Filtered Transactions",
                        "tags", List.of("banking", "current-account", "transactions"),
                        "examples", List.of(
                                "Show transactions from 2026-04-19 to 2026-04-21",
                                "Show last 2 transactions",
                                "What were my largest transactions last week?"))));
        card.put("endpoints", Map.of(
                "execute", Map.of(
                        "url", baseUrl + "/api/account-agent-demo/a2a/execute",
                        "method", "POST",
                        "contentType", "application/json"),
                "health", Map.of(
                        "url", baseUrl + "/health",
                        "method", "GET")));
        card.put("inputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                        "task_input", Map.of("type", "string", "description", "Caller task or request"),
                        "session_id", Map.of("type", "string", "description", "Optional session id for conversation continuity")),
                "required", List.of("task_input")));
        card.put("outputSchema", Map.of(
                "type", "object",
                "properties", Map.of(
                        "role", Map.of("type", "string", "enum", List.of("agent")),
                        "content", Map.of("type", "string"),
                        "tool_calls", Map.of("type", "array"),
                        "session_id", Map.of("type", "string")),
                "required", List.of("role", "content")));
        card.put("metadata", Map.of(
                "framework", "google-adk-java",
                "runtime", "spring-boot",
                "tags", List.of("a2a", "banking", "current-account", "read-only"),
                "toolContract", Map.of(
                        "name", "get-account-summary-and-transactions-filtered",
                        "requiredParameters", List.of("fromDate", "toDate"),
                        "optionalParameters", List.of("numOfTransLimit"),
                        "parameterSemantics", Map.of(
                                "fromDate", "inclusive, YYYY-MM-DD",
                                "toDate", "inclusive, YYYY-MM-DD",
                                "numOfTransLimit", "0=summary only, null=all up to 30, N>0=limit N"),
                        "responseNotes", List.of(
                                "Use availableBalance as default balance answer unless both are explicitly requested",
                                "Transaction descriptions are returned verbatim as provided by the API"))));
        return card;
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        return Map.of("ok", true, "message", "adkAccountAgentDemo: no mock database to reset");
    }
}
