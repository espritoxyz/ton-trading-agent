export type ChatRole = 'USER' | 'SYSTEM'

export interface ChatItem {
    id: string
    role: ChatRole
    content: string
    createdAt: string
    backendMessageId?: string
}

export interface WalletBalances {
    address: string
    balances: Array<{ symbol: string; amount: string }>
}

export interface UserProfile {
    email: string
}
