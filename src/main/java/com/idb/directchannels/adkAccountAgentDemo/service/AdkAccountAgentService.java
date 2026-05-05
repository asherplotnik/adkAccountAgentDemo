package com.idb.directchannels.adkAccountAgentDemo.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.Part;

import io.reactivex.rxjava3.core.Flowable;

@Service
public class AdkAccountAgentService {

    private static final String DEMO_USER_ID = "banking-demo-user";

    private final InMemoryRunner accountPlatformRunner;

    public AdkAccountAgentService(InMemoryRunner accountPlatformRunner) {
        this.accountPlatformRunner = accountPlatformRunner;
    }

    public AgentExecutionResult execute(String sessionId, String taskInput) {
        return getAccountPlatformRunnerOutput(sessionId, taskInput, DEMO_USER_ID);
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

        RunConfig runConfig = RunConfig.builder().build();
        Content userMsg = Content.fromParts(Part.fromText(taskInput));
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
        return new AgentExecutionResult(out.isEmpty() ? null : out.toString(), toolCalls);
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
