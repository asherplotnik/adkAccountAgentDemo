package com.idb.directchannels.adkAccountAgentDemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankAgentExecuteRequest(
        @JsonProperty("task_input") String taskInput,
        @JsonProperty("session_id") String sessionId) {
}
