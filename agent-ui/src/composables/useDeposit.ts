import { ref } from 'vue'
import { api } from './useApi.ts'
import { userId, accessToken } from './useAuth.ts'

export interface DepositRequest {
    depositRequestId: number
    code: string
    depositWalletAddress: string
    expiresAt: string
    status: 'PENDING' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED'
}

export interface DepositStatus {
    depositRequestId: number
    code: string
    status: 'PENDING' | 'COMPLETED' | 'EXPIRED' | 'CANCELLED'
    amountTon: string | null // Renamed but contains readable amount for any asset
    assetType: string | null // "TON" or "JETTON"
    jettonSymbol: string | null
    jettonMasterAddress: string | null
    transactionHash: string | null
    createdAt: string
    expiresAt: string
    completedAt: string | null
}

export const currentDeposit = ref<DepositRequest | null>(null)
export const depositStatus = ref<DepositStatus | null>(null)
export const loadingDeposit = ref(false)
export const depositError = ref<string | null>(null)

export async function initiateDeposit() {
    depositError.value = null
    loadingDeposit.value = true
    try {
        if (!userId.value) {
            throw new Error('User not authenticated')
        }

        const headers: Record<string, string> = {}
        if (accessToken.value) headers.Authorization = `Bearer ${accessToken.value}`

        const { data } = await api.post('/deposit/initiate', {
            userId: userId.value
        }, { headers })

        currentDeposit.value = data
        return data as DepositRequest
    } catch (e: any) {
        depositError.value = e?.message ?? 'Failed to initiate deposit'
        throw e
    } finally {
        loadingDeposit.value = false
    }
}

export async function checkDepositStatus(depositRequestId: number) {
    depositError.value = null
    try {
        const headers: Record<string, string> = {}
        if (accessToken.value) headers.Authorization = `Bearer ${accessToken.value}`

        const { data } = await api.get(`/deposit/${depositRequestId}/status`, { headers })
        depositStatus.value = data
        return data as DepositStatus
    } catch (e: any) {
        depositError.value = e?.message ?? 'Failed to check deposit status'
        throw e
    }
}
