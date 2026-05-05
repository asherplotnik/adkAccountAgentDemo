package com.idb.directchannels.adkAccountAgentDemo.context;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class BankAgentRequestContextHolder {

    private final ThreadLocal<BankAgentRequestContext> requestContextThreadLocal = new ThreadLocal<>();

    public void set(BankAgentRequestContext requestContext) {
        requestContextThreadLocal.set(requestContext);
    }

    public Optional<BankAgentRequestContext> optional() {
        return Optional.ofNullable(requestContextThreadLocal.get());
    }

    public BankAgentRequestContext getOrThrow() {
        BankAgentRequestContext requestContext = requestContextThreadLocal.get();
        if (requestContext == null) {
            throw new IllegalStateException("Missing bank agent request context");
        }
        return requestContext;
    }

    public void clear() {
        requestContextThreadLocal.remove();
    }
}
