export type ChatRole = 'USER' | 'SYSTEM'

export type ChatUtilityKind = 'CONFIRM_SEND_TON'

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
