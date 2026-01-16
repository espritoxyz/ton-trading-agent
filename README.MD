# Запуск и установка

1. Поднять `docker engine`
2. Вставить `.env` в корень репозитория
3. Включить впн на компе, иначе запросы в гпт не улетят
4. В корне команда `docker compose build` и `docker compose up -d`
5. В модуле `agent-ui` команда `npm run dev` чтобы поднять юи

# Roadmap MVP 

1. User Assets Widget [STONFI]
    - [ ] Дизайн и моки @formazon
2. User Orders Widget (👀 2026) 
    - написать какие параметры по ордерам
    - Дизайн [STONFI]
    - Реализация
3. User Transactions [STONFI]
    - история транзакций 
    - пополнения
    - hyperlink (zishkaz)
4. User Account [STONFI]
    - [x] Sign in  
    - [ ] Sign up (email)
        - [ ] Добавить email verification no-reply@esprito.app
5. Deposit Assets [STONFI+]
    - добавить рецепт пополнить баланс
    - отправить в чат сообщение пополнен баланс
6. Withdraw Assets [STONFI] 
    - send_ton_to_address
    - send_token_to_address
7. Chat Notifications (👀 2026) 
    - надо подумать какие события улетают в чат
8. Analytics
    - Google Analytics или Yandex.Metrika
9. Infrastructure
    - app.stating.esprito.app
    - app.esprito.app


[Описание рецептов](RECIPES.MD)
