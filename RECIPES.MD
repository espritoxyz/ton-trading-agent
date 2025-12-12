
# Esprito Recipes

## TODO

-[ ] `lookup_assets(address: Address): List<Asset>`
-[x] `get_ton_to_usdt_exchange_rate(): Double`
-[ ] `get_token_to_ton_exchange_rate(jettonMaster: Address): Double`
-[x] `send_ton_to_address(userId: Long, amount: Double, receiver: Address): SuccessReport | ErrorReport`
-[ ] `send_token_to_address(userId: Long, jettonMaster: Address, amount: Double, receiver: Address): SuccessReport | ErrorReport`
-[ ] `swap_ton_to_token(userId: Long, jettonMaster: Address, minimalTokenAmount: Double): SuccessReport | ErrorReport`
-[ ] `swap_token_to_ton(userId: Long, jettonMaster: Address, swappedTokenAmount: Double): SuccessReport | ErrorReport`
-[ ] `smart_buy(...)`
-[ ] `smart_sell(...)`

## MVP recipes

### 1. `lookup_assets(address: Address): List<Asset>`

**Описание:** Получает список всех активов (TON и токенов) по указанному адресу кошелька. Возвращает балансы TON и всех jettons, найденных на этом адресе.

**Примеры использования:**

- "Покажи баланс кошелька EQD...xyz"
- "Какие активы есть на адресе 0:abc...def?"
- "Проверь баланс моего кошелька"

**Возвращает:** Список активов с указанием типа (TON или jetton), количества и адреса контракта токена (для jettons).

---

### 2. `get_ton_to_usdt_exchange_rate(): Double`

**Описание:** Получает текущий курс обмена TON к USDT. Используется для конвертации сумм в долларах и оценки стоимости транзакций.

**Примеры использования:**

- "Сколько стоит 1 TON в долларах?"
- "Какой курс TON к USDT?"
- "Переведи 100 TON в доллары"

**Возвращает:** Число с плавающей точкой, представляющее количество USDT за 1 TON.

---

### 3. `get_token_to_ton_exchange_rate(jettonMaster: Address): Double`

**Описание:** Получает текущий курс обмена указанного токена (jetton) к TON. Необходим для оценки стоимости токенов и планирования обменов.

**Примеры использования:**

- "Сколько TON стоит 1 JETTON по адресу EQD...xyz?"
- "Какой курс токена USDT к TON?"
- "Переведи 1000 токенов в TON"

**Параметры:**

- `jettonMaster` - адрес мастер-контракта токена (jetton master address)

**Возвращает:** Число с плавающей точкой, представляющее количество TON за 1 единицу токена.

---

### 4. `send_ton_to_address(userId: Long, amount: Double, receiver: Address): SuccessReport | ErrorReport`

**Описание:** Отправляет указанное количество TON с кошелька пользователя на указанный адрес получателя. Требует подтверждения перед выполнением.

**Примеры использования:**

- "Отправь 10 TON на адрес EQD...xyz"
- "Переведи 5.5 TON пользователю 0:abc...def"
- "Отправь 100 TON на кошелек друга"

**Параметры:**

- `userId` - идентификатор пользователя (автоматически определяется из контекста)
- `amount` - количество TON для отправки
- `receiver` - адрес получателя в формате TON (EQD... или 0:...)

**Возвращает:** Отчет об успехе с хешем транзакции или отчет об ошибке с описанием проблемы.

---

### 5. `send_token_to_address(userId: Long, jettonMaster: Address, amount: Double, receiver: Address): SuccessReport | ErrorReport`

**Описание:** Отправляет указанное количество токена (jetton) с кошелька пользователя на указанный адрес получателя. Работает аналогично отправке TON, но для токенов.

**Примеры использования:**

- "Отправь 1000 USDT на адрес EQD...xyz"
- "Переведи 500 токенов JETTON на 0:abc...def"
- "Отправь 50 токенов на кошелек EQD...xyz"

**Параметры:**

- `userId` - идентификатор пользователя
- `jettonMaster` - адрес мастер-контракта токена
- `amount` - количество токенов для отправки
- `receiver` - адрес получателя

**Возвращает:** Отчет об успехе или ошибке с деталями транзакции.

---

### 6. `swap_ton_to_token(userId: Long, jettonMaster: Address, minimalTokenAmount: Double): SuccessReport | ErrorReport`

**Описание:** Обменивает TON на указанный токен через DEX (децентрализованную биржу). Параметр `minimalTokenAmount` задает минимальное количество токенов, которое должно быть получено (защита от проскальзывания).

**Примеры использования:**

- "Купи USDT на 10 TON, минимум 50 USDT"
- "Обменяй 5 TON на токен JETTON, минимум 100 токенов"
- "Свапни 20 TON в USDT с минимальным выходом 100 USDT"

**Параметры:**

- `userId` - идентификатор пользователя
- `jettonMaster` - адрес мастер-контракта токена, который нужно купить
- `minimalTokenAmount` - минимальное количество токенов, которое должно быть получено (защита от проскальзывания)

**Возвращает:** Отчет об успехе с деталями обмена или отчет об ошибке.

---

### 7. `swap_token_to_ton(userId: Long, jettonMaster: Address, swappedTokenAmount: Double): SuccessReport | ErrorReport`

**Описание:** Обменивает указанное количество токена на TON через DEX. Позволяет конвертировать токены обратно в TON.

**Примеры использования:**

- "Продай 100 USDT за TON"
- "Обменяй 500 токенов JETTON на TON"
- "Свапни 50 USDT в TON"

**Параметры:**

- `userId` - идентификатор пользователя
- `jettonMaster` - адрес мастер-контракта токена, который нужно продать
- `swappedTokenAmount` - количество токенов для обмена

**Возвращает:** Отчет об успехе с количеством полученного TON или отчет об ошибке.

---

### 8. `smart_buy(...)`

**Описание:** Умная покупка токена с автоматическим расчетом оптимального количества TON для обмена, выбором лучшего DEX и защитой от проскальзывания. Параметры могут включать целевое количество токенов, максимальную сумму TON, допустимое проскальзывание и другие оптимизации.

**Примеры использования:**

- "Купи токен JETTON на 100 TON с минимальным проскальзыванием"
- "Умная покупка USDT на сумму до 50 TON"
- "Купи 1000 токенов оптимальным способом"

**Возможные параметры:**

- Целевое количество токенов или максимальная сумма в TON
- Допустимое проскальзывание
- Выбор DEX (автоматический или ручной)
- Deadline для транзакции

---

### 9. `smart_sell(...)`

**Описание:** Умная продажа токена с автоматическим поиском лучшего курса, выбором оптимального DEX и защитой от неблагоприятных условий рынка. Может включать параметры для частичной продажи, стоп-лосс и другие стратегии.

**Примеры использования:**

- "Продай все мои токены JETTON оптимальным способом"
- "Умная продажа 50% USDT за TON"
- "Продай токены с минимальным проскальзыванием"

**Возможные параметры:**

- Количество токенов для продажи (или процент от баланса)
- Минимальная сумма TON, которую нужно получить
- Допустимое проскальзывание
- Выбор DEX

---

## Planned recipes

### Операции с ликвидностью

- `add_liquidity(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, amountB: Double): SuccessReport | ErrorReport`
  - Добавление ликвидности в пул DEX для получения комиссий
  - Пример: "Добавь ликвидность в пул TON/USDT: 100 TON и 5000 USDT"

- `remove_liquidity(userId: Long, lpToken: Address, amount: Double): SuccessReport | ErrorReport`
  - Удаление ликвидности из пула и получение токенов обратно
  - Пример: "Выведи ликвидность из пула, 50 LP токенов"

### Стейкинг и депозиты

- `stake_ton(userId: Long, validator: Address, amount: Double): SuccessReport | ErrorReport`
  - Стейкинг TON у валидатора для получения наград
  - Пример: "Застейкай 1000 TON у валидатора EQD...xyz"

- `unstake_ton(userId: Long, stakeAddress: Address): SuccessReport | ErrorReport`
  - Вывод застейканных TON обратно
  - Пример: "Выведи мои застейканные TON"

- `deposit_to_savings(userId: Long, protocol: Address, amount: Double): SuccessReport | ErrorReport`
  - Депозит в протоколы сбережений для получения процентов
  - Пример: "Депозит 500 TON в протокол сбережений"

### NFT операции

- `transfer_nft(userId: Long, nftAddress: Address, receiver: Address): SuccessReport | ErrorReport`
  - Передача NFT на другой адрес
  - Пример: "Отправь NFT на адрес EQD...xyz"

- `list_nft_for_sale(userId: Long, nftAddress: Address, price: Double, currency: Address): SuccessReport | ErrorReport`
  - Выставление NFT на продажу на маркетплейсе
  - Пример: "Выставь NFT на продажу за 100 TON"

- `buy_nft(userId: Long, nftAddress: Address, maxPrice: Double): SuccessReport | ErrorReport`
  - Покупка NFT с маркетплейса
  - Пример: "Купи NFT, максимум 50 TON"

### Управление кошельками

- `create_subwallet(userId: Long): SuccessReport | ErrorReport`
  - Создание подкошелька для организации средств
  - Пример: "Создай новый подкошелек"

- `deploy_wallet(userId: Long, walletType: String): SuccessReport | ErrorReport`
  - Деплой кошелька на блокчейн (если еще не задеплоен)
  - Пример: "Задеплой мой кошелек v4r2"

### Мониторинг и аналитика

- `get_transaction_history(userId: Long, limit: Int, offset: Int): List<Transaction>`
  - Получение истории транзакций пользователя
  - Пример: "Покажи последние 10 транзакций"

- `get_portfolio_value(userId: Long): PortfolioReport`
  - Оценка общей стоимости портфеля в USDT
  - Пример: "Сколько стоит мой портфель?"

- `track_price_alert(token: Address, targetPrice: Double, direction: String): SuccessReport`
  - Установка алерта на изменение цены токена
  - Пример: "Уведоми меня, когда USDT упадет ниже 0.99 TON"

### Продвинутые обмены

- `multi_hop_swap(userId: Long, fromToken: Address, toToken: Address, amount: Double): SuccessReport | ErrorReport`
  - Обмен через несколько пулов для лучшего курса
  - Пример: "Обменяй токен A на токен B через несколько пулов"

- `limit_order(userId: Long, tokenA: Address, tokenB: Address, amountA: Double, minAmountB: Double): SuccessReport | ErrorReport`
  - Размещение лимитного ордера на DEX
  - Пример: "Купи USDT за TON, когда курс будет 0.95 или лучше"

### Взаимодействие со смарт-контрактами

- `call_contract(userId: Long, contractAddress: Address, method: String, params: Map<String, Any>): SuccessReport | ErrorReport`
  - Вызов метода смарт-контракта
  - Пример: "Вызови метод claim() в контракте EQD...xyz"

- `deploy_contract(userId: Long, contractCode: String, initData: String): SuccessReport | ErrorReport`
  - Деплой собственного смарт-контракта
  - Пример: "Задеплой этот контракт с начальными данными"

### Социальные функции

- `send_ton_with_comment(userId: Long, amount: Double, receiver: Address, comment: String): SuccessReport | ErrorReport`
  - Отправка TON с комментарием (для платежей с описанием)
  - Пример: "Отправь 10 TON с комментарием 'Оплата за обед'"

- `split_payment(userId: Long, receivers: List<Address>, amounts: List<Double>): SuccessReport | ErrorReport`
  - Разделение платежа между несколькими получателями
  - Пример: "Отправь 10 TON на адрес A, 20 TON на адрес B"
