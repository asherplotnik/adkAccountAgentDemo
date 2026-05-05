package com.idb.directchannels.adkAccountAgentDemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.google.adk.agents.BaseAgent;
import com.google.adk.models.BaseLlm;
import com.google.adk.models.Gemini;
import com.google.adk.runner.InMemoryRunner;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.idb.directchannels.adkAccountAgentDemo.agent.AccountPlatformAgent;
import com.idb.directchannels.adkAccountAgentDemo.tools.AccountSummaryAndTransactionsFilteredToolService;

@Configuration
public class AdkAgentConfiguration {

    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public BaseLlm accountPlatformGemini(
            @Value("${spring.ai.google.genai.api-key:}") String apiKey,
            @Value("${spring.ai.google.genai.base-url:}") String baseUrl,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash}") String model) {
        Client.Builder clientBuilder = Client.builder();
        if (!apiKey.isBlank()) {
            clientBuilder.apiKey(apiKey);
        }
        if (!baseUrl.isBlank()) {
            clientBuilder.httpOptions(
                    HttpOptions.builder().baseUrl(baseUrl).apiVersion("").build());
        }
        return Gemini.builder().modelName(model).apiClient(clientBuilder.build()).build();
    }

    @Bean
    public BaseAgent accountPlatformRootAgent(
            BaseLlm accountPlatformGemini,
            AccountSummaryAndTransactionsFilteredToolService accountSummaryAndTransactionsFilteredToolService) {
        return AccountPlatformAgent.createRoot(
                accountPlatformGemini,
                accountSummaryAndTransactionsFilteredToolService);
    }

    @Bean
    public InMemoryRunner accountPlatformRunner(BaseAgent accountPlatformRootAgent) {
        return new InMemoryRunner(accountPlatformRootAgent);
    }
}
