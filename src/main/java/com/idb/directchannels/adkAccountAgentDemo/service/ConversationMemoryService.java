package com.idb.directchannels.adkAccountAgentDemo.service;

import java.util.List;

import com.idb.directchannels.adkAccountAgentDemo.model.SessionMessage;

public interface ConversationMemoryService {
    List<SessionMessage> getHistory(String sessionId);

    void appendUserMessage(String sessionId, String content);

    void appendAssistantMessage(String sessionId, String content);
}
