#!/bin/bash
# Script to import tasks from KANBAN.MD to GitHub Issues
# Usage: ./scripts/import-kanban-to-issues.sh [--dry-run]

set -e

REPO="espritoxyz/ton-trading-agent"
DRY_RUN=false

# Parse arguments
if [[ "$1" == "--dry-run" ]]; then
    DRY_RUN=true
    echo "🔍 DRY RUN MODE - No issues will be created"
    echo ""
fi

# Check if gh is installed and authenticated
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) is not installed"
    echo "Install it from: https://cli.github.com/"
    exit 1
fi

if ! gh auth status &> /dev/null; then
    echo "❌ GitHub CLI is not authenticated"
    echo "Run: gh auth login"
    exit 1
fi

echo "📋 Importing tasks from KANBAN.MD to GitHub Issues"
echo "Repository: $REPO"
echo ""

# Function to create issue
create_issue() {
    local title="$1"
    local body="$2"
    local labels="$3"
    
    if [ "$DRY_RUN" = true ]; then
        echo "  [DRY RUN] Would create: $title"
        echo "    Labels: $labels"
    else
        echo "  Creating: $title"
        gh issue create \
            --repo "$REPO" \
            --title "$title" \
            --body "$body" \
            --label "$labels" 2>&1 | grep -E "(Creating issue|#[0-9]+)" || echo "    ✓ Created"
    fi
}

# Create labels if they don't exist
echo "🏷️  Ensuring labels exist..."
for label in "kanban:todo" "kanban:backlog" "mvp" "recipe" "2026"; do
    if [ "$DRY_RUN" = false ]; then
        gh label create "$label" --repo "$REPO" --force 2>/dev/null || true
    fi
done
echo ""

# === TO DO TASKS ===
echo "📋 Creating TO DO tasks..."
echo ""

echo "→ User Assets Widget"
create_issue \
    "User Assets Widget: Дизайн и моки" \
    "Создать дизайн и моки для виджета отображения активов пользователя.

**Assignee:** @formazon

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

create_issue \
    "User Assets Widget: Реализация виджета" \
    "Реализовать виджет для отображения активов пользователя.

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

echo ""
echo "→ User Orders Widget (2026)"
create_issue \
    "User Orders Widget: Параметры по ордерам" \
    "Написать какие параметры по ордерам нужно отображать.

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,mvp,enhancement"

create_issue \
    "User Orders Widget: Разработка функционала" \
    "Разработать функционал отображения ордеров пользователя.

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,mvp,enhancement"

echo ""
echo "→ User Transactions"
create_issue \
    "User Transactions: Отображение пополнений" \
    "Реализовать отображение пополнений в интерфейсе.

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

create_issue \
    "User Transactions: Hyperlink" \
    "Добавить hyperlink для транзакций.

**Assignee:** @zishkaz

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

echo ""
echo "→ User Account - Sign up"
create_issue \
    "User Account: Регистрация через email" \
    "Реализовать функционал регистрации пользователей через email.

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

create_issue \
    "User Account: Email verification" \
    "Добавить email verification no-reply@esprito.app

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

echo ""
echo "→ Deposit Assets"
create_issue \
    "Deposit Assets: Рецепт пополнения баланса" \
    "Добавить рецепт \"пополнить баланс\".

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,recipe,enhancement"

create_issue \
    "Deposit Assets: Уведомление в чат" \
    "Отправить в чат сообщение о пополнении баланса.

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

echo ""
echo "→ Withdraw Assets (2026)"
create_issue \
    "Withdraw Assets: Интеграция send_token_to_address" \
    "Интеграция функции отправки токенов в UI: \`send_token_to_address\`

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,mvp,recipe,enhancement"

echo ""
echo "→ Chat Notifications (2026)"
create_issue \
    "Chat Notifications: Определить события" \
    "Определить какие события улетают в чат.

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,mvp,enhancement"

create_issue \
    "Chat Notifications: Реализация системы" \
    "Реализация системы уведомлений в чат.

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,mvp,enhancement"

echo ""
echo "→ Analytics"
create_issue \
    "Analytics: Интеграция Google Analytics или Yandex.Metrika" \
    "Интегрировать Google Analytics или Yandex.Metrika для отслеживания использования приложения.

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,enhancement"

echo ""
echo "→ Infrastructure"
create_issue \
    "Infrastructure: app.staging.esprito.app" \
    "Настроить staging окружение app.staging.esprito.app

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,infrastructure"

create_issue \
    "Infrastructure: app.esprito.app" \
    "Настроить production окружение app.esprito.app

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,infrastructure"

echo ""
echo "→ Recipes Implementation - Priority"
create_issue \
    "Recipe: lookup_assets" \
    "Реализовать рецепт \`lookup_assets(address: Address): List<Asset>\`

**Описание:** Получение списка активов по адресу

**Примеры использования:**
- \"Покажи баланс кошелька\"
- \"Какие активы есть на адресе?\"

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,recipe,enhancement"

create_issue \
    "Recipe: send_token_to_address" \
    "Реализовать рецепт \`send_token_to_address(userId: Long, jettonMaster: Address, amount: Double, receiver: Address)\`

**Описание:** Отправка токенов на адрес

**Примеры использования:**
- \"Отправь 1000 USDT на адрес\"
- \"Переведи токены\"

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,recipe,enhancement"

create_issue \
    "Recipe: swap_token_to_ton" \
    "Реализовать рецепт \`swap_token_to_ton(userId: Long, jettonMaster: Address, swappedTokenAmount: Double)\`

**Описание:** Обмен токена на TON

**Примеры использования:**
- \"Продай 100 USDT за TON\"
- \"Обменяй токены на TON\"

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,recipe,enhancement"

create_issue \
    "Recipe: decide_jetton_master" \
    "Реализовать рецепт \`decide_jetton_master(ticker: String): Address\`

**Описание:** Определение адреса мастер-контракта по тикеру

**Примеры использования:**
- \"Найди адрес контракта для USDT\"

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,mvp,recipe,enhancement"

echo ""
echo "→ Recipes Implementation - 2026 (Smart Trading)"
create_issue \
    "Recipe: smart_buy" \
    "Реализовать рецепт \`smart_buy(...)\`

**Описание:** Умная покупка токена с:
- Автоматическим расчетом оптимального количества TON
- Выбором лучшего DEX
- Защитой от проскальзывания

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,recipe,enhancement"

create_issue \
    "Recipe: smart_sell" \
    "Реализовать рецепт \`smart_sell(...)\`

**Описание:** Умная продажа токена с:
- Автоматическим поиском лучшего курса
- Выбором оптимального DEX
- Стратегиями частичной продажи и стоп-лосс

**Метка:** 👀 2026

**Источник:** KANBAN.MD - To Do" \
    "kanban:todo,2026,recipe,enhancement"

# === BACKLOG TASKS ===
echo ""
echo ""
echo "📦 Creating BACKLOG tasks..."
echo ""

echo "→ Операции с ликвидностью"
create_issue \
    "Recipe: add_liquidity" \
    "Реализовать рецепт \`add_liquidity(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, amountB: Double)\`

**Описание:** Добавление ликвидности в пул DEX для получения комиссий

**Пример:** \"Добавь ликвидность в пул TON/USDT: 100 TON и 5000 USDT\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: remove_liquidity" \
    "Реализовать рецепт \`remove_liquidity(userId: Long, lpToken: Address, amount: Double)\`

**Описание:** Удаление ликвидности из пула и получение токенов обратно

**Пример:** \"Выведи ликвидность из пула, 50 LP токенов\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Стейкинг и депозиты"
create_issue \
    "Recipe: stake_ton" \
    "Реализовать рецепт \`stake_ton(userId: Long, validator: Address, amount: Double)\`

**Описание:** Стейкинг TON у валидатора для получения наград

**Пример:** \"Застейкай 1000 TON у валидатора EQD...xyz\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: unstake_ton" \
    "Реализовать рецепт \`unstake_ton(userId: Long, stakeAddress: Address)\`

**Описание:** Вывод застейканных TON обратно

**Пример:** \"Выведи мои застейканные TON\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: deposit_to_savings" \
    "Реализовать рецепт \`deposit_to_savings(userId: Long, protocol: Address, amount: Double)\`

**Описание:** Депозит в протоколы сбережений для получения процентов

**Пример:** \"Депозит 500 TON в протокол сбережений\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ NFT операции"
create_issue \
    "Recipe: transfer_nft" \
    "Реализовать рецепт \`transfer_nft(userId: Long, nftAddress: Address, receiver: Address)\`

**Описание:** Передача NFT на другой адрес

**Пример:** \"Отправь NFT на адрес EQD...xyz\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: list_nft_for_sale" \
    "Реализовать рецепт \`list_nft_for_sale(userId: Long, nftAddress: Address, price: Double, currency: Address)\`

**Описание:** Выставление NFT на продажу на маркетплейсе

**Пример:** \"Выставь NFT на продажу за 100 TON\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: buy_nft" \
    "Реализовать рецепт \`buy_nft(userId: Long, nftAddress: Address, maxPrice: Double)\`

**Описание:** Покупка NFT с маркетплейса

**Пример:** \"Купи NFT, максимум 50 TON\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Управление кошельками"
create_issue \
    "Recipe: create_subwallet" \
    "Реализовать рецепт \`create_subwallet(userId: Long)\`

**Описание:** Создание подкошелька для организации средств

**Пример:** \"Создай новый подкошелек\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: deploy_wallet" \
    "Реализовать рецепт \`deploy_wallet(userId: Long, walletType: String)\`

**Описание:** Деплой кошелька на блокчейн (если еще не задеплоен)

**Пример:** \"Задеплой мой кошелек v4r2\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Мониторинг и аналитика"
create_issue \
    "Recipe: get_transaction_history" \
    "Реализовать рецепт \`get_transaction_history(userId: Long, limit: Int, offset: Int)\`

**Описание:** Получение истории транзакций пользователя

**Пример:** \"Покажи последние 10 транзакций\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: get_portfolio_value" \
    "Реализовать рецепт \`get_portfolio_value(userId: Long)\`

**Описание:** Оценка общей стоимости портфеля в USDT

**Пример:** \"Сколько стоит мой портфель?\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: track_price_alert" \
    "Реализовать рецепт \`track_price_alert(token: Address, targetPrice: Double, direction: String)\`

**Описание:** Установка алерта на изменение цены токена

**Пример:** \"Уведоми меня, когда USDT упадет ниже 0.99 TON\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Продвинутые обмены"
create_issue \
    "Recipe: multi_hop_swap" \
    "Реализовать рецепт \`multi_hop_swap(userId: Long, fromToken: Address, toToken: Address, amount: Double)\`

**Описание:** Обмен через несколько пулов для лучшего курса

**Пример:** \"Обменяй токен A на токен B через несколько пулов\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: limit_order" \
    "Реализовать рецепт \`limit_order(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, minAmountB: Double)\`

**Описание:** Размещение лимитного ордера на DEX

**Пример:** \"Купи USDT за TON, когда курс будет 0.95 или лучше\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Взаимодействие со смарт-контрактами"
create_issue \
    "Recipe: call_contract" \
    "Реализовать рецепт \`call_contract(userId: Long, contractAddress: Address, method: String, params: Map<String, Any>)\`

**Описание:** Вызов метода смарт-контракта

**Пример:** \"Вызови метод claim() в контракте EQD...xyz\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: deploy_contract" \
    "Реализовать рецепт \`deploy_contract(userId: Long, contractCode: String, initData: String)\`

**Описание:** Деплой собственного смарт-контракта

**Пример:** \"Задеплой этот контракт с начальными данными\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo "→ Социальные функции"
create_issue \
    "Recipe: send_ton_with_comment" \
    "Реализовать рецепт \`send_ton_with_comment(userId: Long, amount: Double, receiver: Address, comment: String)\`

**Описание:** Отправка TON с комментарием (для платежей с описанием)

**Пример:** \"Отправь 10 TON с комментарием 'Оплата за обед'\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

create_issue \
    "Recipe: split_payment" \
    "Реализовать рецепт \`split_payment(userId: Long, receivers: List<Address>, amounts: List<Double>)\`

**Описание:** Разделение платежа между несколькими получателями

**Пример:** \"Отправь 10 TON на адрес A, 20 TON на адрес B\"

**Источник:** KANBAN.MD - Backlog" \
    "kanban:backlog,recipe,enhancement"

echo ""
echo ""
echo "✅ Done!"
echo ""
echo "📊 Summary:"
echo "  - TO DO: 22 issues"
echo "  - BACKLOG: 20 issues"
echo "  - TOTAL: 42 issues"
echo ""
echo "💡 Next steps:"
echo "  1. Review the created issues in GitHub"
echo "  2. Add them to your GitHub Project manually or use GitHub's auto-add feature"
echo "  3. Adjust labels, assignees, and priorities as needed"
echo ""
