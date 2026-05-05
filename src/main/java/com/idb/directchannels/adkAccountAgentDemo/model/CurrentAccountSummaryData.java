package com.idb.directchannels.adkAccountAgentDemo.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CurrentAccountSummaryData(
        String branchNumber,
        String accountNumber,
        Double balance,
        Double availableBalance,
        String currencyCode,
        String currencyDescription,
        Double creditLineFramework,
        Boolean secureFutureTransactionsExists,
        Boolean loanExists,
        Boolean termDepositExists,
        Boolean savingPlansExists,
        Boolean loanTermDepositExists,
        Boolean securityExists,
        Boolean mortgageExists,
        Double parameterMinTransactionsForDisplay,
        Boolean isInLegalTreatment,
        List<CurrentAccountTransactionItem> transactionsList) {
}
