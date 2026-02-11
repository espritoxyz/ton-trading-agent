import { ref } from 'vue'
import { api } from './useApi.ts'
import { userId, accessToken } from './useAuth.ts'

export interface DepositSession {
    walletAddress: string
    expiresAt: string
    message: string
}

export const currentDeposit = ref<DepositSession | null>(null)
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
        return data as DepositSession
    } catch (e: any) {
        depositError.value = e?.message ?? 'Failed to initiate deposit'
        throw e
    } finally {
        loadingDeposit.value = false
    }
}
