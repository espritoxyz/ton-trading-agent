# Kanban Board - ton-trading-agent-MVP

## 🎯 Project Overview
Kanban board для проекта ton-trading-agent-MVP. Содержит задачи из README.MD и RECIPES.MD.

---

## ✅ Done (Завершено)

### User Account - Sign in
- ✅ Реализована функция входа в систему
- Статус: Завершено

### Recipes Implementation
- ✅ `get_ton_to_usdt_exchange_rate(): Double` - Получение курса TON к USDT
- ✅ `get_token_to_ton_exchange_rate(jettonMaster: Address): Double` - Получение курса токена к TON
- ✅ `send_ton_to_address(userId: Long, amount: Double, receiver: Address)` - Отправка TON на адрес
- ✅ `swap_ton_to_token(userId: Long, jettonMaster: Address, minimalTokenAmount: Double)` - Обмен TON на токен

---

## 🚧 In Progress (В работе)

_Задачи, над которыми идет активная работа_

---

## 📋 To Do (К выполнению)

### User Assets Widget
- [ ] Дизайн и моки (@formazon)
- [ ] Реализация виджета для отображения активов пользователя

### User Orders Widget (👀 2026)
- [ ] Написать какие параметры по ордерам
- [ ] Разработать функционал отображения ордеров

### User Transactions
- [ ] Реализация отображения пополнений
- [ ] Добавить hyperlink (zishkaz)

### User Account - Sign up
- [ ] Регистрация через email
- [ ] Добавить email verification no-reply@esprito.app

### Deposit Assets
- [ ] Добавить рецепт "пополнить баланс"
- [ ] Отправить в чат сообщение о пополнении баланса

### Withdraw Assets (👀 2026)
- [ ] Реализация функции отправки TON: `send_ton_to_address`
- [ ] Реализация функции отправки токенов: `send_token_to_address`

### Chat Notifications (👀 2026)
- [ ] Определить какие события улетают в чат
- [ ] Реализация системы уведомлений

### Analytics
- [ ] Интеграция Google Analytics или Yandex.Metrika

### Infrastructure
- [ ] Настроить app.staging.esprito.app
- [ ] Настроить app.esprito.app

### Recipes Implementation - Priority
- [ ] `lookup_assets(address: Address): List<Asset>` - Получение списка активов по адресу
  - Примеры: "Покажи баланс кошелька", "Какие активы есть на адресе?"
  
- [ ] `send_token_to_address(userId: Long, jettonMaster: Address, amount: Double, receiver: Address)` - Отправка токенов
  - Примеры: "Отправь 1000 USDT на адрес", "Переведи токены"
  
- [ ] `swap_token_to_ton(userId: Long, jettonMaster: Address, swappedTokenAmount: Double)` - Обмен токена на TON
  - Примеры: "Продай 100 USDT за TON", "Обменяй токены на TON"
  
- [ ] `decide_jetton_master(ticker: String): Address` - Определение адреса мастер-контракта по тикеру
  - Примеры: "Найди адрес контракта для USDT"

### Recipes Implementation - 2026 (Smart Trading)
- [ ] `smart_buy(...)` - Умная покупка токена
  - Автоматический расчет оптимального количества TON
  - Выбор лучшего DEX
  - Защита от проскальзывания
  
- [ ] `smart_sell(...)` - Умная продажа токена
  - Автоматический поиск лучшего курса
  - Выбор оптимального DEX
  - Стратегии частичной продажи и стоп-лосс

---

## 📦 Backlog (Будущие задачи)

### Операции с ликвидностью
- [ ] `add_liquidity(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, amountB: Double)` - Добавление ликвидности в пул DEX
- [ ] `remove_liquidity(userId: Long, lpToken: Address, amount: Double)` - Удаление ликвидности из пула

### Стейкинг и депозиты
- [ ] `stake_ton(userId: Long, validator: Address, amount: Double)` - Стейкинг TON у валидатора
- [ ] `unstake_ton(userId: Long, stakeAddress: Address)` - Вывод застейканных TON
- [ ] `deposit_to_savings(userId: Long, protocol: Address, amount: Double)` - Депозит в протоколы сбережений

### NFT операции
- [ ] `transfer_nft(userId: Long, nftAddress: Address, receiver: Address)` - Передача NFT
- [ ] `list_nft_for_sale(userId: Long, nftAddress: Address, price: Double, currency: Address)` - Выставление NFT на продажу
- [ ] `buy_nft(userId: Long, nftAddress: Address, maxPrice: Double)` - Покупка NFT

### Управление кошельками
- [ ] `create_subwallet(userId: Long)` - Создание подкошелька
- [ ] `deploy_wallet(userId: Long, walletType: String)` - Деплой кошелька на блокчейн

### Мониторинг и аналитика
- [ ] `get_transaction_history(userId: Long, limit: Int, offset: Int)` - История транзакций
- [ ] `get_portfolio_value(userId: Long)` - Оценка стоимости портфеля
- [ ] `track_price_alert(token: Address, targetPrice: Double, direction: String)` - Алерты на изменение цены

### Продвинутые обмены
- [ ] `multi_hop_swap(userId: Long, fromToken: Address, toToken: Address, amount: Double)` - Обмен через несколько пулов
- [ ] `limit_order(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, minAmountB: Double)` - Лимитные ордера

### Взаимодействие со смарт-контрактами
- [ ] `call_contract(userId: Long, contractAddress: Address, method: String, params: Map<String, Any>)` - Вызов метода контракта
- [ ] `deploy_contract(userId: Long, contractCode: String, initData: String)` - Деплой контракта

### Социальные функции
- [ ] `send_ton_with_comment(userId: Long, amount: Double, receiver: Address, comment: String)` - Отправка TON с комментарием
- [ ] `split_payment(userId: Long, receivers: List<Address>, amounts: List<Double>)` - Разделение платежа

---

## 📊 Statistics

- **Done**: 5 задач
- **In Progress**: 0 задач
- **To Do**: 23 задачи
- **Backlog**: 20 задач

**Total**: 48 задач

---

## 🔗 References

- [README.MD](README.MD) - Roadmap MVP и инструкции по запуску
- [RECIPES.MD](RECIPES.MD) - Подробное описание рецептов

---

## 📝 Notes

- Задачи с пометкой (👀 2026) планируются к реализации в 2026 году
- Приоритетные задачи находятся в разделе "To Do"
- Долгосрочные задачи находятся в "Backlog"
