package com.idb.directchannels.adkAccountAgentDemo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentAccountTransactionItem(
        Integer transactionNumber,
        String transactionCode,
        String transactionDescription,
        String transactionFullDescription,
        String transactionDate,
        String transactionBusinessDate,
        Double transactionAmount) {
}
