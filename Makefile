# Makefile
SHELL := /bin/bash
COMPOSE := docker compose
UI_DIR := agent-ui
NPM := npm
ENV_FILE := .env

.PHONY: help env-check build up down restart ui-dev ui-build start stop logs clean

help:
	@printf "Commands:\n"
	@printf "  make env-check   - ensure `%s` exists\n" "$(ENV_FILE)"
	@printf "  make build       - run `$(COMPOSE) build`\n"
	@printf "  make up          - run `$(COMPOSE) up -d`\n"
	@printf "  make down        - run `$(COMPOSE) down --volumes --remove-orphans`\n"
	@printf "  make restart     - down then up\n"
	@printf "  make ui-dev      - run UI dev server in `%s`\n" "$(UI_DIR)"
	@printf "  make ui-build    - build UI\n"
	@printf "  make start       - env-check + build + up + ui-dev\n"
	@printf "  make stop        - stop compose\n"
	@printf "  make logs        - follow `docker compose` logs\n"
	@printf "  make clean       - down and remove volumes\n"

env-check:
	@test -f $(ENV_FILE) || (printf "Error: `%s` not found\n" "$(ENV_FILE)" >&2; exit 1)

build:
	@$(COMPOSE) build

up:
	@$(COMPOSE) up -d

down:
	@$(COMPOSE) down --volumes --remove-orphans

restart: down up

ui-dev:
	@cd $(UI_DIR) && $(NPM) install && $(NPM) run dev

ui-build:
	@cd $(UI_DIR) && $(NPM) ci && $(NPM) run build

start: env-check build up ui-dev

stop: down

logs:
	@$(COMPOSE) logs -f

clean: down
