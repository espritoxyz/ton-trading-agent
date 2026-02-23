# Esprito AI — TON Trading Agent

> AI-powered trading agent for the TON blockchain. Chat naturally with an LLM that executes on-chain operations on your behalf.

## What it does

- **Chat-driven trading** — describe what you want in plain language; the agent figures out the on-chain steps
- **Token swaps** — buy/sell tokens via Ston.fi DEX (TON ↔ Jetton, Jetton ↔ TON)
- **Transfers** — send TON or any Jetton to any address
- **Price queries** — real-time TON/USDT and token exchange rates
- **Limit orders** — place and track conditional orders

## Getting started

### Prerequisites

- Docker Engine + Docker Compose
- Node.js ≥ 18 and npm ≥ 9 (for frontend build)
- A `.env` file in the project root 
- Access to OpenAI API (or a compatible proxy)

### Running locally

```bash
# 1. Build all Docker images
make build

# 2. Start all backend services (PostgreSQL, Redis, RabbitMQ, Keycloak, backend, recipe processor, Nginx)
make up

# 3. Build the frontend (outputs to ui-dist/, served by Nginx)
cd agent-ui && npm ci && npm run build

# Follow logs
make logs
```

The app will be available at `http://localhost` (port 8080, served by Nginx).

### Stopping

```bash
make down        # stop containers
make clean       # stop and remove all volumes (destructive)
```

## Development

### Backend tests

```bash
gradle :agent-backend:test                        # run all tests
gradle :agent-backend:test --tests "*.SomeTest"   # single test class
```

### Frontend

```bash
cd agent-ui
npm run build          # compile TypeScript + Vite build → ui-dist/
npm run dev            # start Vite dev server
```

## License

Business Source License 1.1 — see [LICENSE](LICENSE).
