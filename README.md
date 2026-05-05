# adkAccountAgentDemo

Specialized ADK-based **Current Account Agent** used in an A2A setup.

This service is designed to be called by other agents (especially `adkDemo`) and focuses only on:
- current-account summary
- filtered transactions

It is read-only.

## What It Exposes

- `POST /api/account-agent-demo/a2a/execute`
  - A2A execute endpoint
  - Input body:
    - `task_input` (required)
    - `session_id` (optional)
  - Returns:
    - `role`
    - `content`
    - `tool_calls`
    - `session_id`

- `GET /.well-known/agent.json`
  - Agent card / capability metadata for discovery and demo

- `GET /health`
  - Health check

## Agent Behavior (High-Level)

- Agent name: `accountPlatformSpecialist`
- Domain: current-account only (summary + transactions)
- Language policy: agent-to-agent communication is in English
- Balance rule: use `availableBalance` by default unless explicitly asked for both balances
- Transaction values: preserve original API text verbatim (including Hebrew descriptions)

## Tool

The agent uses:
- `get-account-summary-and-transactions-filtered`

Tool behavior:
- `fromDate` optional (defaults to today)
- `toDate` optional (defaults to today)
- `numOfTransLimit` optional
  - `0` => summary only (no transactions)
  - `null` => all transactions (up to API cap)
  - `N > 0` => max `N` transactions

## Configuration

Key properties (see `src/main/resources/application.yaml`):

- `server.port=8081`
- `banking.tools.current-account.base-url=http://localhost:3000`
- `spring.ai.google.genai.*` model connector settings

## Run Locally

From `adkAccountAgentDemo`:

```bash
./mvnw spring-boot:run
```

Service starts on:
- `http://localhost:8081`

## Integration With Main Agent

`adkDemo` can delegate current-account requests to this service over A2A by calling:
- `http://localhost:8081/api/account-agent-demo/a2a/execute`

For local multi-service runs:
- Start `adkAccountAgentDemo` on `8081`
- Start `adkDemo` on `8080`
- Ensure upstream bank API/mock is reachable on configured base URL (default `localhost:3000`)
