package com.idb.directchannels.adkAccountAgentDemo.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.idb.directchannels.adkAccountAgentDemo.context.BankAgentRequestContext;
import com.idb.directchannels.adkAccountAgentDemo.context.BankAgentRequestContextHolder;
import com.idb.directchannels.adkAccountAgentDemo.model.BankAgentExecuteRequest;
import com.idb.directchannels.adkAccountAgentDemo.model.BankAgentExecuteResponse;
import com.idb.directchannels.adkAccountAgentDemo.service.AdkAccountAgentService.AgentExecutionResult;
import com.idb.directchannels.adkAccountAgentDemo.service.AdkAccountAgentService;

@RestController
public class AdkAccountAgentProxy {

    private final AdkAccountAgentService adkAccountAgentService;
    private final BankAgentRequestContextHolder requestContextHolder;

    public AdkAccountAgentProxy(
            AdkAccountAgentService adkAccountAgentService,
            BankAgentRequestContextHolder requestContextHolder) {
        this.adkAccountAgentService = adkAccountAgentService;
        this.requestContextHolder = requestContextHolder;
    }

    @PostMapping("/api/account-agent-demo/a2a/execute")
    public BankAgentExecuteResponse execute(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "sessionId", defaultValue = "SessionID") String sessionId,
            @RequestHeader(value = "X-Global-Transaction-ID", defaultValue = "netanel1122334455ss") String globalTransactionId,
            @RequestHeader(value = "Accept-Language", defaultValue = "he-IL") String acceptLanguage,
            @RequestHeader(value = "clientOS", defaultValue = "1") String clientOS,
            @RequestHeader(value = "clientVersion", defaultValue = "2") String clientVersion,
            @RequestHeader(value = "X-Forwarded-For", defaultValue = "123456") String xForwardedFor,
            @RequestHeader(value = "accountV", defaultValue = "blabla") String accountV,
            @RequestBody BankAgentExecuteRequest requestBody) {
        String resolvedSessionId = requestBody.sessionId() == null || requestBody.sessionId().isBlank()
                ? sessionId
                : requestBody.sessionId();
        requestContextHolder.set(new BankAgentRequestContext(
                authorization,
                resolvedSessionId,
                globalTransactionId,
                acceptLanguage,
                clientOS,
                clientVersion,
                xForwardedFor,
                accountV));
        try {
            AgentExecutionResult result = adkAccountAgentService.execute(resolvedSessionId, requestBody.taskInput());
            return new BankAgentExecuteResponse("agent", result.content(), result.toolCalls(), resolvedSessionId);
        } finally {
            requestContextHolder.clear();
        }
    }
}
