package com.idb.directchannels.adkAccountAgentDemo.tools;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.google.adk.tools.Annotations.Schema;
import com.idb.directchannels.adkAccountAgentDemo.context.BankAgentRequestContext;
import com.idb.directchannels.adkAccountAgentDemo.context.BankAgentRequestContextHolder;
import com.idb.directchannels.adkAccountAgentDemo.model.CurrentAccountSummary;
import com.idb.directchannels.adkAccountAgentDemo.model.CurrentAccountSummaryAndTransactionsResponse;
import com.idb.directchannels.adkAccountAgentDemo.model.CurrentAccountSummaryData;
import com.idb.directchannels.adkAccountAgentDemo.util.JwtUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AccountSummaryAndTransactionsFilteredToolService {

    private final RestClient restClient;
    private final String currentAccountBaseUrl;
    private final BankAgentRequestContextHolder requestContextHolder;

    public AccountSummaryAndTransactionsFilteredToolService(
            RestClient.Builder restClientBuilder,
            @Value("${banking.tools.current-account.base-url:http://localhost:3000}") String currentAccountBaseUrl,
            BankAgentRequestContextHolder requestContextHolder) {
        this.restClient = restClientBuilder.build();
        this.currentAccountBaseUrl = currentAccountBaseUrl.replaceAll("/+$", "");
        this.requestContextHolder = requestContextHolder;
    }

    @Schema(
            name = "get-account-summary-and-transactions-filtered",
            description = """
                    Use this tool to retrieve current-account summary with filtered transactions for the authenticated customer.
                    Uses request context headers (Authorization, transaction/language/client headers).
                    Transaction date fields (transactionDate, transactionBusinessDate) are returned in YYYYMMDD format.
                    fromDate and toDate are optional. If both are omitted, defaults to current month start through today.
                    """)
    public CurrentAccountSummaryAndTransactionsResponse getAccountSummaryAndTransactionsFiltered(
            @Schema(description = "Inclusive start date in YYYY-MM-DD format (optional; if both dates omitted defaults to first day of current month)", optional = true) String fromDate,
            @Schema(description = "Inclusive end date in YYYY-MM-DD format (optional; if both dates omitted defaults to today)", optional = true) String toDate,
            @Schema(
                            description =
                                    "Max transactions to return; 0 returns summary only (no transactions), null returns all transactions (up to 30)",
                            optional = true)
                    Integer numOfTransLimit) {
        BankAgentRequestContext requestContext = requestContextHolder.getOrThrow();
        log.info(
                "[TOOL][account-summary-transactions-filtered][START] fromDate={} toDate={} limit={} sessionId={} globalTxId={}",
                fromDate,
                toDate,
                numOfTransLimit,
                requestContext.sessionId(),
                requestContext.globalTransactionId());

        LocalDate today = LocalDate.now();
        String resolvedFromDate = (fromDate == null || fromDate.isBlank()) ? null : fromDate.trim();
        String resolvedToDate = (toDate == null || toDate.isBlank()) ? null : toDate.trim();
        if (resolvedFromDate == null && resolvedToDate == null) {
            resolvedFromDate = today.withDayOfMonth(1).toString();
            resolvedToDate = today.toString();
        } else if (resolvedFromDate == null) {
            resolvedFromDate = resolvedToDate;
        } else if (resolvedToDate == null) {
            resolvedToDate = resolvedFromDate;
        }
        final String finalFromDate = resolvedFromDate;
        final String finalToDate = resolvedToDate;

        String endpoint = currentAccountBaseUrl + "/api/v1/currentAccount/currentAccountSummaryFiltered";
        String branchNumber = JwtUtils.getBranchNumber(requestContext.authorization());
        String accountNumber = JwtUtils.getAccountNumber(requestContext.authorization());

        try {
            String uriTemplate = endpoint + "?fromDate={fromDate}&toDate={toDate}";
            if (numOfTransLimit != null) {
                uriTemplate += "&numOfTransLimit={numOfTransLimit}";
            }
            CurrentAccountSummaryAndTransactionsResponse response = numOfTransLimit != null
                    ? restClient.get()
                            .uri(uriTemplate, finalFromDate, finalToDate, numOfTransLimit)
                            .header(HttpHeaders.AUTHORIZATION, requestContext.authorization())
                            .header("X-Global-Transaction-ID", requestContext.globalTransactionId())
                            .header("Accept-Language", requestContext.acceptLanguage())
                            .header("clientOS", requestContext.clientOS())
                            .header("clientVersion", requestContext.clientVersion())
                            .retrieve()
                            .body(CurrentAccountSummaryAndTransactionsResponse.class)
                    : restClient.get()
                            .uri(uriTemplate, finalFromDate, finalToDate)
                            .header(HttpHeaders.AUTHORIZATION, requestContext.authorization())
                            .header("X-Global-Transaction-ID", requestContext.globalTransactionId())
                            .header("Accept-Language", requestContext.acceptLanguage())
                            .header("clientOS", requestContext.clientOS())
                            .header("clientVersion", requestContext.clientVersion())
                            .retrieve()
                            .body(CurrentAccountSummaryAndTransactionsResponse.class);
            return enrichWithDerivedAccountData(response, branchNumber, accountNumber);
        } catch (RestClientResponseException ex) {
            throw new RuntimeException(
                    "Filtered current account summary API failed (" + ex.getStatusCode().value() + " " + ex.getStatusText()
                            + "): " + ex.getResponseBodyAsString(),
                    ex);
        }
    }

    private static CurrentAccountSummaryAndTransactionsResponse enrichWithDerivedAccountData(
            CurrentAccountSummaryAndTransactionsResponse response,
            String branchNumber,
            String accountNumber) {
        if (response == null || response.currentAccountSummary() == null || response.currentAccountSummary().data() == null) {
            return response;
        }

        CurrentAccountSummary summary = response.currentAccountSummary();
        CurrentAccountSummaryData data = summary.data();
        CurrentAccountSummaryData enrichedData = new CurrentAccountSummaryData(
                branchNumber,
                accountNumber,
                data.balance(),
                data.availableBalance(),
                data.currencyCode(),
                data.currencyDescription(),
                data.creditLineFramework(),
                data.secureFutureTransactionsExists(),
                data.loanExists(),
                data.termDepositExists(),
                data.savingPlansExists(),
                data.loanTermDepositExists(),
                data.securityExists(),
                data.mortgageExists(),
                data.parameterMinTransactionsForDisplay(),
                data.isInLegalTreatment(),
                data.transactionsList());

        return new CurrentAccountSummaryAndTransactionsResponse(
                new CurrentAccountSummary(enrichedData, summary.metaData()));
    }
}
