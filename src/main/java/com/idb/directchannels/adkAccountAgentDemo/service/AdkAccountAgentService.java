package com.idb.directchannels.adkAccountAgentDemo.service;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;
import com.idb.directchannels.adkAccountAgentDemo.model.SessionMessage;

import io.reactivex.rxjava3.core.Flowable;

@Slf4j
@Service
public class AdkAccountAgentService {

    private static final String DEMO_USER_ID = "banking-demo-user";
    private static final String SESSION_DATE_ANCHOR_PREFIX = "SESSION_CONTEXT:";
    private static final ZoneId SESSION_DATE_ZONE = ZoneId.of("Asia/Jerusalem");

    private final InMemoryRunner accountPlatformRunner;
    private final boolean dumpSessionMemory;
    private final ConversationMemoryService conversationMemoryService;
    private final ConversationPromptBuilder conversationPromptBuilder;

    public AdkAccountAgentService(
            InMemoryRunner accountPlatformRunner,
            ConversationMemoryService conversationMemoryService,
            ConversationPromptBuilder conversationPromptBuilder,
            @Value("${banking.debug.dump-session-memory:true}") boolean dumpSessionMemory) {
        this.accountPlatformRunner = accountPlatformRunner;
        this.conversationMemoryService = conversationMemoryService;
        this.conversationPromptBuilder = conversationPromptBuilder;
        this.dumpSessionMemory = dumpSessionMemory;
    }

    public AgentExecutionResult execute(String sessionId, String taskInput) {
        ensureSessionDateAnchor(sessionId);
        AgentExecutionResult result = getAccountPlatformRunnerOutput(sessionId, taskInput, DEMO_USER_ID);
        if (taskInput != null && !taskInput.isBlank()) {
            conversationMemoryService.appendUserMessage(sessionId, taskInput);
        }
        if (result.content() != null && !result.content().isBlank()) {
            conversationMemoryService.appendAssistantMessage(sessionId, result.content());
        }
        return result;
    }

    private void ensureSessionDateAnchor(String sessionId) {
        List<SessionMessage> history = conversationMemoryService.getHistory(sessionId);
        boolean hasAnchor = history.stream()
                .map(SessionMessage::content)
                .filter(content -> content != null && !content.isBlank())
                .anyMatch(content -> content.startsWith(SESSION_DATE_ANCHOR_PREFIX));
        if (hasAnchor) {
            return;
        }
        String today = LocalDate.now(SESSION_DATE_ZONE).toString();
        String anchor = "SESSION_CONTEXT: today=" + today + ", timezone=" + SESSION_DATE_ZONE
                + ". Use this as the reference date for all relative date calculations.";
        conversationMemoryService.appendAssistantMessage(sessionId, anchor);
        log.info("[MEMORY][SESSION_CONTEXT][SET] sessionId={} anchor=\"{}\"", sessionId, anchor);
    }

    private AgentExecutionResult getAccountPlatformRunnerOutput(String sessionId, String taskInput, String userId) {
        String appName = accountPlatformRunner.appName();
        Session session = accountPlatformRunner
                .sessionService()
                .getSession(appName, userId, sessionId, Optional.empty())
                .switchIfEmpty(
                        accountPlatformRunner
                                .sessionService()
                                .createSession(appName, userId, Map.of(), sessionId)
                                .toMaybe())
                .blockingGet();
        log.info(
                "[DEBUG][SESSION_DUMP][ACCOUNT][INPUT] sessionId={} adkSessionId={} fullPayloadStart\n{}\n[DEBUG][SESSION_DUMP][ACCOUNT][INPUT] fullPayloadEnd",
                sessionId,
                session.id(),
                taskInput == null ? "null" : taskInput);
        dumpSessionSnapshot("ACCOUNT", "BEFORE_RUN", session, userId);

        RunConfig runConfig = RunConfig.builder().build();
        String promptWithHistory = conversationPromptBuilder.buildPromptWithHistory(sessionId, taskInput);
        if (dumpSessionMemory) {
            log.info(
                    "[DEBUG][CUSTOM_MEMORY][ACCOUNT][PROMPT] sessionId={} fullPayloadStart\n{}\n[DEBUG][CUSTOM_MEMORY][ACCOUNT][PROMPT] fullPayloadEnd",
                    sessionId,
                    promptWithHistory);
        }
        Content userMsg = Content.fromParts(Part.fromText(promptWithHistory));
        Flowable<Event> events =
                accountPlatformRunner.runAsync(session.userId(), session.id(), userMsg, runConfig);

        StringBuilder out = new StringBuilder();
        List<Object> toolCalls = new ArrayList<>();
        events.blockingForEach(event -> {
            collectToolCalls(event, toolCalls);
            if (event.finalResponse()) {
                out.append(event.stringifyContent());
            }
        });
        log.info(
                "[DEBUG][SESSION_DUMP][ACCOUNT][OUTPUT] sessionId={} adkSessionId={} fullPayloadStart\n{}\n[DEBUG][SESSION_DUMP][ACCOUNT][OUTPUT] fullPayloadEnd",
                sessionId,
                session.id(),
                out.isEmpty() ? "null" : out.toString());
        Session refreshedSession = accountPlatformRunner
                .sessionService()
                .getSession(appName, userId, sessionId, Optional.empty())
                .blockingGet();
        dumpSessionSnapshot("ACCOUNT", "AFTER_RUN", refreshedSession, userId);
        return new AgentExecutionResult(out.isEmpty() ? null : out.toString(), toolCalls);
    }

    private void dumpSessionSnapshot(String agent, String phase, Session session, String userId) {
        if (!dumpSessionMemory || !log.isInfoEnabled()) {
            return;
        }
        String[] candidateMethods = {"state", "events", "messages", "history", "turns", "memory"};
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("appName", accountPlatformRunner.appName());
        details.put("userId", userId);
        details.put("sessionId", session.id());
        details.put("sessionClass", session.getClass().getName());
        details.put("sessionToString", String.valueOf(session));

        for (String methodName : candidateMethods) {
            invokeNoArgMethod(session, methodName).ifPresent(value -> details.put(methodName, value));
        }
        log.info(
                "[DEBUG][SESSION_MEMORY][{}][{}] fullPayloadStart\n{}\n[DEBUG][SESSION_MEMORY][{}][{}] fullPayloadEnd",
                agent,
                phase,
                details,
                agent,
                phase);
    }

    private static Optional<Object> invokeNoArgMethod(Session session, String methodName) {
        try {
            Method method = session.getClass().getMethod(methodName);
            Object value = method.invoke(session);
            return Optional.ofNullable(value);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static void collectToolCalls(Event event, List<Object> toolCalls) {
        for (FunctionCall fc : event.functionCalls()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function_call");
            item.put("toolName", fc.name().orElse("?"));
            item.put("callId", fc.id().orElse("-"));
            item.put("args", fc.args().orElse(null));
            toolCalls.add(item);
        }
        for (FunctionResponse fr : event.functionResponses()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "function_response");
            item.put("toolName", fr.name().orElse("?"));
            item.put("callId", fr.id().orElse("-"));
            item.put("result", fr.response().orElse(null));
            toolCalls.add(item);
        }
    }

    public record AgentExecutionResult(String content, List<Object> toolCalls) {
    }
}
