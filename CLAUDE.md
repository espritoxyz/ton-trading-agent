# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Esprito AI** — an AI-powered TON blockchain trading agent. Users chat with an LLM that executes on-chain operations (send TON/tokens, swap via Ston.fi DEX, create limit orders) on their behalf.

This is a Kotlin/Spring Boot backend + Vue.js/TypeScript frontend project with TON blockchain integration, Docker Compose deployment, PostgreSQL, Redis, and RabbitMQ. Check application.yaml and docker-compose.yml for infrastructure config before claiming something is configured correctly.

## Security section
Never inject raw user/LLM content into HTML. Any approach that puts untrusted content into innerHTML or similar is an XSS risk. Linkification and formatting must happen on the frontend with proper sanitization.

## General Guidelines section
When proposing fixes, do not suggest quick/shallow fixes unless explicitly asked. Default to production-ready, industry-standard solutions. Plan before implementing complex changes.

## Commands

### Docker Compose (all backend services)
Everything except the frontend runs via Docker Compose:
```bash
# Requires .env in project root
make build          # docker compose build
make up             # docker compose up -d
make build && make up   # Full rebuild and start
make down           # Stop
make restart        # down + up
make logs           # Follow docker-compose logs
```

### Backend tests
```bash
gradle :agent-backend:test                           # Run all backend tests
gradle :agent-backend:test --tests "*.SomeTest"      # Run a single test class
```

### Frontend
The frontend is verified via build and type-check — **never run `npm run dev`**, as it spins up a separate dev server. Instead:
```bash
cd agent-ui
npm run build                   # Compile TypeScript + Vite build → ui-dist/ (served by nginx)
npx vue-tsc --noEmit            # Type-check without producing output
```

## Architecture

### Module structure
- **`agent-llm/`** — Pure Kotlin library. Defines the LLM interaction layer: `OpenAIChatter`, tool interfaces (`AgentTool`, `BlockchainAdapter`), and all tool DTOs/implementations. No Spring dependency.
- **`agent-backend/`** — Spring Boot 3.3 application. Depends on `agent-llm`. Hosts REST/WebSocket API, DB, RabbitMQ listeners, and implements `BlockchainAdapter` via `AgentBlockchainAdapter`.
- **`agent-ui/`** — Vue 3 + TypeScript SPA (Vite). Talks to `agent-backend` over HTTP/WebSocket.
- **`recipe-processor-node/`** — Node.js TypeScript service. Executes actual TON blockchain operations (send TON/token, swap via Ston.fi) using `@ton/ton` and `@ston-fi/sdk`.

### Infrastructure (docker-compose)
- **PostgreSQL** — app DB (port 25431) + Keycloak DB (port 25432)
- **RabbitMQ** — async event bus (AMQP 5672, management UI 15672)
- **Redis** — price data cache, Stonfi pools/assets cache
- **Keycloak** — OAuth2/OIDC identity provider (port 18080)
- **Nginx** — reverse proxy + serves built Vue assets from `ui-dist/`

### Request flow: User chat message → on-chain result
1. `ChatController` receives user message → `ChatJobService.submit()` queues a job
2. `ChatJobService` creates/reuses `OpenAIChatter` (one per user, maintains LLM history in memory)
3. `OpenAIChatter` calls LLM via `ai-router` library (`gpt-5-mini` model); tool calls that require confirmation suspend via `CompletableDeferred` and wait for `ConfirmationController` approval
4. Blockchain tools in `AgentBlockchainAdapter` publish events to RabbitMQ exchange `app.events` with routing key `agent-llm.<action>` (e.g. `agent-llm.swap-ton-to-token`)
5. **`recipe-processor-node`** consumes `agent-llm.#` events and performs the actual on-chain transaction via TON SDK + Ston.fi SDK
6. Node.js publishes result back to `app.events` (e.g. `agent-llm.swap-ton-to-token.result`)
7. `AgentEventsListener` (Spring) receives result → calls `ExternalToolResultService.complete()` which resolves the suspended coroutine in the chatter
8. `OpenAIChatter.summarizeToolCalls()` passes the result to the LLM for a human-readable reply
9. `ChatJobService.status()` returns the completed reply to the polling UI

### Key design decisions
- **Tool confirmation flow**: Tools implementing `ConfirmationRequired` pause the LLM loop and surface a confirmation item to the UI via `ConfirmationService`. The `ChatJobService` holds a `CompletableDeferred<Boolean>` per pending confirmation.
- **Async result correlation**: `ExternalToolResultService` maps `(messageId, toolName) → CompletableDeferred<String>`. `AgentBlockchainAdapter.awaitExternalResults()` awaits these with a 60s timeout.
- **Wallet security**: User mnemonics are AES-encrypted (via `EncryptionService`) before DB storage. Decrypted only in `AgentBlockchainAdapter` right before publishing to RabbitMQ, and included in the RabbitMQ payload for `recipe-processor-node`.
- **Stonfi caches**: `StonfiAssetsCacheService` and `StonfiPoolsCacheService` cache DEX asset and pool data in Redis. Used to look up jetton decimals, USD prices, and best pools for swaps.
- **Chat history**: Per-user `OpenAIChatter` instance is cached in `ChatJobService.userIdToChatter`. History is not persisted to DB between server restarts.
- **LLM library**: Uses the private `com.explyt.ai.router:ai-router` library from GitHub Packages. Requires `GITHUB_USERNAME` and `GITHUB_PAT` env vars for resolution.

### RabbitMQ routing (exchange: `app.events`, type: topic)
| Routing key pattern | Consumer |
|---|---|
| `agent-llm.#` | `recipe-processor-node` + `agent-backend` (AgentEventsListener) |
| `wallet.#` | `agent-backend` (WalletEventsListener) |
| `deposit.#` | `agent-backend` (DepositEventsListener) + `recipe-processor-node` |

### Database schema (Flyway migrations in `agent-backend/src/main/resources/db/migration/`)
Key entities: `AgentUser`, `UserWallet`, `Order`, `PriceTracker`, `Notification`, `WalletTransaction`, `BalanceTransaction`, `OfflineToken`, `EmailVerificationToken`.

### Adding a new LLM tool
1. Create `ArgsDto` in `agent-llm/.../tool/dto/`
2. Create `AgentTool` impl in `agent-llm/.../tool/impl/` (implement `ConfirmationRequired` if user approval needed)
3. Add abstract method to `BlockchainAdapter` in `agent-llm`
4. Register in `ToolDefinitions.allTools`
5. Implement the method in `AgentBlockchainAdapter` (agent-backend)
6. If async (blockchain op): publish RabbitMQ event, handle result in `AgentEventsListener`, call `externalToolResultService.complete()`
7. If a new on-chain operation: add handler in `recipe-processor-node/src/index.ts`

## UI/Frontend Guidelines section
When fixing UI/CSS issues, investigate the root cause (stacking contexts, border box model, render order) before applying surface-level fixes like z-index increases or simple class additions. First attempts with shallow fixes have repeatedly failed.

When adding icons to the UI, always prefer Lucide icons (already available via `lucide-vue-next`) over emojis or other icon sets. Do not use emoji characters as UI icons.

## Domain Rules section
Always convert blockchain values (nano, wei, etc.) to human-readable format before displaying. Never show raw on-chain values like 149501718 — convert to 0.149 TON etc.

## GitHub section
Use the GitHub CLI (`gh`) for all GitHub-related tasks: reviewing PRs, creating PRs, viewing issues, checking CI status, commenting, etc. Do not open GitHub URLs in a browser or rely on the web API directly when `gh` can do the job.

## Code Quality section
Do not add unnecessary dependencies. Before introducing a new library (Caffeine, Guava, etc.), check if the existing stack already provides equivalent functionality. Ask before adding dependencies.

## Documentation and References section
When in doubt about library APIs, framework behavior, or best practices, use the MCP context7 tool to fetch up-to-date documentation. Prefer context7 over guessing or relying on potentially outdated training knowledge.
