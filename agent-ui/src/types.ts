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

// Unified Wallet State API types
export interface WalletStateResponse {
    userId: number
    balance: BalanceData
    assets: AssetData[]
    transactions: TransactionData[]
    orders: OrderData[]
    metadata: WalletStateMetadata
}

export interface BalanceData {
    totalUsd: number
    lastUpdated: string
}

export interface AssetData {
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

export interface TransactionData {
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

export type OrderAction = 'buy' | 'sell'

export type PriceDirection = 'UP' | 'DOWN'

export interface OrderData {
    id: number
    jettonMaster: string
    action: OrderAction
    amount: number
    createdAt: string
    fulfilled: boolean
    symbol?: string
    targetPrice?: number
    direction?: PriceDirection
}

export interface WalletStateMetadata {
    fromCache: boolean
    cacheAge: number | null
    transactionCount: number
    transactionsLimit: number
    activeOrdersCount: number
    fulfilledOrdersCount: number
}

// Notification types
export type NotificationType =
    | 'BALANCE_CHANGE'
    | 'TRANSACTION_COMPLETE'
    | 'SWAP_EXECUTED'
    | 'ORDER_FILLED'
    | 'TRACKER_TRIGGERED'

export interface Notification {
    id: number
    type: NotificationType
    title: string
    message: string
    metadata: NotificationMetadata
    isRead: boolean
    createdAt: string
    readAt?: string | null
}

// Metadata type interfaces for each notification type
export interface BalanceChangeMetadata {
    amount: number
    currency: string
    newBalance?: number
    transactionId?: string
}

export interface TransactionCompleteMetadata {
    transactionId: string
    status: 'success' | 'failed'
    amount?: number
    currency?: string
    recipientAddress?: string
    errorReason?: string
}

export interface SwapExecutedMetadata {
    fromAsset: string
    toAsset: string
    fromAmount: number
    toAmount: number
    executionPrice?: number
    slippagePercent?: number
    transactionId?: string
}

export interface OrderFilledMetadata {
    orderId: number
    jettonMaster?: string
    action?: string
    amount?: number
    targetPrice?: number
    fillType: 'full' | 'partial' | 'cancelled'
    filledQuantity?: number
    remainingQuantity?: number
    status?: string
    reason?: string
}

export interface TrackerTriggeredMetadata {
    trackerId: number
    jettonMaster: string
    symbol: string
    targetPrice: string
    direction: 'UP' | 'DOWN'
}

// Union type for all metadata types
export type NotificationMetadata =
    | BalanceChangeMetadata
    | TransactionCompleteMetadata
    | SwapExecutedMetadata
    | OrderFilledMetadata
    | TrackerTriggeredMetadata

// Utility functions to parse and validate notification metadata
export function parseNotificationMetadata<T extends NotificationMetadata>(
    notification: Notification
): T | null {
    try {
        if (typeof notification.metadata === 'string') {
            return JSON.parse(notification.metadata) as T
        }
        return notification.metadata as T
    } catch (e) {
        console.error('Failed to parse notification metadata:', e)
        return null
    }
}

export function isBalanceChangeMetadata(
    metadata: NotificationMetadata
): metadata is BalanceChangeMetadata {
    return 'amount' in metadata && 'currency' in metadata
}

export function isTransactionCompleteMetadata(
    metadata: NotificationMetadata
): metadata is TransactionCompleteMetadata {
    return 'transactionId' in metadata && 'status' in metadata
}

export function isSwapExecutedMetadata(
    metadata: NotificationMetadata
): metadata is SwapExecutedMetadata {
    return 'fromAsset' in metadata && 'toAsset' in metadata && 'fromAmount' in metadata && 'toAmount' in metadata
}

export function isOrderFilledMetadata(
    metadata: NotificationMetadata
): metadata is OrderFilledMetadata {
    return 'orderId' in metadata && 'fillType' in metadata
}
