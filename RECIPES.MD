## MVP recipes

* `lookup_assets(address: Address): List<Asset>`
* `get_ton_to_usdt_exchange_rate(): Double` + 
* `get_token_to_ton_exchange_rate(jettonMaster: Address): Double`
* `send_ton_to_address(userId: Long, amount: Double, receiver: Address): SuccessReport | ErrorReport` +
* `send_token_to_address(userId: Long, jettonMaster: Address, amount: Double, receiver: Address): SuccessReport | ErrorReport`
* `swap_ton_to_token(userId: Long, jettonMaster: Address, minimalTokenAmount: Double): SuccessReport | ErrorReport` 
* `swap_token_to_ton(userId: Long, jettonMaster: Address, swappedTokenAmount: Double): SuccessReport | ErrorReport` 
* `smart_buy(...)`
* `smart_sell(...)`

## Planned recipes 
