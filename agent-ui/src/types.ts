export type ChatRole = 'USER' | 'SYSTEM'

export type ChatUtilityKind = 'CONFIRM_SEND_TON' | 'SHOW_TOP_UP'

export interface ChatItem {
    id: string
    role: ChatRole
    content: string
    createdAt: string
    backendMessageId?: string
    // Optional UI utility widget kind and metadata
    utilityKind?: ChatUtilityKind
    utilityMeta?: Record<string, any>
}

export interface WalletBalances {
    address: string
    balances: Array<{ symbol: string; amount: string }>
}

export interface UserProfile {
    email: string
}

export interface Asset {
    id: number
    address: string
    amountNano: string
    symbol?: string
    decimals?: number
    name?: string
    imageUrl?: string
    readableAmount?: string
    unitPrice?: number
    usdValue?: number
}

export interface JettonMetadata {
    address: string
    name: string
    symbol: string
    decimals: number
    image: string
    verification?: string
}

export type TransactionDirection = 'INCOMING' | 'OUTGOING'

export type AssetType = 'TON' | 'JETTON'

export interface Transaction {
    id: number
    transactionHash: string
    transactionLt: number
    direction: TransactionDirection
    amountNano: string
    assetType: AssetType
    jettonMasterAddress?: string
    jettonSymbol?: string
    jettonDecimals?: number
    senderAddress?: string
    recipientAddress?: string
    comment?: string
    createdAt: string
}

export interface TransactionHistoryResponse {
    transactions: Transaction[]
    total?: number
}
