package com.idb.directchannels.adkAccountAgentDemo.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;

public final class JwtUtils {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private JwtUtils() {
    }

    public static String getBranchNumber(String jwt) {
        String accountId = getAccountIdFromClaims(getJwtClaims(jwt));
        return splitAccountId(accountId)[0];
    }

    public static String getAccountNumber(String jwt) {
        String accountId = getAccountIdFromClaims(getJwtClaims(jwt));
        return splitAccountId(accountId)[1];
    }

    private static Map<String, Object> getJwtClaims(String jwt) {
        String token = jwt.replaceFirst("(?i)^Bearer\\s+", "").trim();
        String[] parts = token.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format: expected header.payload.signature");
        }
        if (parts[1] == null || parts[1].isBlank()) {
            throw new IllegalArgumentException("Invalid JWT: payload is missing");
        }

        String payloadJson = decodeBase64Url(parts[1]);
        try {
            return JSON_MAPPER.readValue(payloadJson, MAP_TYPE);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse JWT payload claims: " + ex.getMessage(), ex);
        }
    }

    private static String getAccountIdFromClaims(Map<String, Object> claims) {
        Object accountObj = claims.get("account");
        if (!(accountObj instanceof Map<?, ?> accountClaim)) {
            throw new IllegalArgumentException("JWT account claim is missing accountId/accountID");
        }

        Object accountIdObj = accountClaim.get("accountId");
        if (!(accountIdObj instanceof String) || ((String) accountIdObj).isBlank()) {
            accountIdObj = accountClaim.get("accountID");
        }
        if (!(accountIdObj instanceof String accountId) || accountId.isBlank()) {
            throw new IllegalArgumentException("JWT account claim is missing accountId/accountID");
        }
        return accountId;
    }

    private static String[] splitAccountId(String accountId) {
        String[] parts = accountId.split("-", 2);
        if (parts.length < 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("JWT accountId/accountID is not in expected BRANCH-ACCOUNT format");
        }
        return parts;
    }

    private static String decodeBase64Url(String value) {
        byte[] decoded = Base64.getUrlDecoder().decode(value);
        return new String(decoded, StandardCharsets.UTF_8);
    }
}
