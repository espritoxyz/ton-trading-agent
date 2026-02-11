import { ref } from 'vue'
import { api } from './useApi.ts'
import { accessToken } from './useAuth.ts'

export interface WalletInfo {
    walletAddress: string
    walletVersion: string
    workchain: number
    createdAt: string
    lastUsedAt: string | null
    isActive: boolean
}

export interface Transaction {
    id: number
    transactionHash: string
    transactionLt: number
    direction: 'INCOMING' | 'OUTGOING'
    amountNano: number
    assetType: string
    jettonMasterAddress: string | null
    jettonSymbol: string | null
    jettonDecimals: number | null
    senderAddress: string | null
    recipientAddress: string | null
    comment: string | null
    createdAt: string
}

export const walletInfo = ref<WalletInfo | null>(null)
export const loadingWallet = ref(false)
export const walletError = ref<string | null>(null)

export async function fetchWalletInfo() {
    walletError.value = null
    loadingWallet.value = true
    try {
        const headers: Record<string, string> = {}
        if (accessToken.value) headers.Authorization = `Bearer ${accessToken.value}`

        const { data } = await api.get('/wallet/info', { headers })
        walletInfo.value = data
        return data as WalletInfo
    } catch (e: any) {
        walletError.value = e?.message ?? 'Failed to fetch wallet info'
        throw e
    } finally {
        loadingWallet.value = false
    }
}
