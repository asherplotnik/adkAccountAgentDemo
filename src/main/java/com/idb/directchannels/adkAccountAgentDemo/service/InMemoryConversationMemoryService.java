package com.idb.directchannels.adkAccountAgentDemo.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.idb.directchannels.adkAccountAgentDemo.model.SessionMessage;

@Service
public class InMemoryConversationMemoryService implements ConversationMemoryService {
    private static final int MAX_MESSAGES_PER_SESSION = 20;

    private final ConcurrentMap<String, Deque<SessionMessage>> sessionMemory = new ConcurrentHashMap<>();

    @Override
    public List<SessionMessage> getHistory(String sessionId) {
        Deque<SessionMessage> history = sessionMemory.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (history) {
            return new ArrayList<>(history);
        }
    }

    @Override
    public void appendUserMessage(String sessionId, String content) {
        appendMessage(sessionId, "user", content);
    }

    @Override
    public void appendAssistantMessage(String sessionId, String content) {
        appendMessage(sessionId, "assistant", content);
    }

    private void appendMessage(String sessionId, String role, String content) {
        Deque<SessionMessage> history = sessionMemory.computeIfAbsent(sessionId, key -> new ArrayDeque<>());
        synchronized (history) {
            history.addLast(new SessionMessage(role, content));
            while (history.size() > MAX_MESSAGES_PER_SESSION) {
                history.removeFirst();
            }
        }
    }
}
