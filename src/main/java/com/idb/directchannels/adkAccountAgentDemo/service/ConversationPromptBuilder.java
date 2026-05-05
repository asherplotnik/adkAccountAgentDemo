package com.idb.directchannels.adkAccountAgentDemo.service;

import java.util.List;

import org.springframework.stereotype.Component;

import com.idb.directchannels.adkAccountAgentDemo.model.SessionMessage;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ConversationPromptBuilder {
    private final ConversationMemoryService conversationMemoryService;

    public String buildPromptWithHistory(String sessionId, String requestBody) {
        List<SessionMessage> history = conversationMemoryService.getHistory(sessionId);
        if (history.isEmpty()) {
            return requestBody;
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("Conversation history (same user session):\n");
        for (SessionMessage message : history) {
            prompt.append(message.role()).append(": ").append(message.content()).append('\n');
        }
        prompt.append("\nCurrent user message:\n").append(requestBody);
        return prompt.toString();
    }
}
