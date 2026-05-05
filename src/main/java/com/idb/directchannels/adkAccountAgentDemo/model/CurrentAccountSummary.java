package com.idb.directchannels.adkAccountAgentDemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentAccountSummary(
        CurrentAccountSummaryData data,
        MetaData metaData) {
}
