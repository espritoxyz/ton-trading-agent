import { ref } from 'vue'
import { api } from './useApi.ts'
import { userId, refreshProfile, accessToken } from './useAuth.ts'

export const balanceUsd = ref<number | null>(null)
export const loadingBalance = ref(false)
export const balanceError = ref<string | null>(null)

export async function refreshBalance() {
    balanceError.value = null
    loadingBalance.value = true
    try {
        // ensure we have userId
        if (!userId.value) {
            await refreshProfile()
        }
        if (!userId.value) {
            balanceUsd.value = null
            return
        }
        const headers: Record<string, string> = {}
        if (accessToken.value) headers.Authorization = `Bearer ${accessToken.value}`
        const { data } = await api.get(`/user/${userId.value}/balance`, { headers })
        balanceUsd.value = typeof data?.totalUsd === 'number' ? data.totalUsd : Number(data?.totalUsd ?? 0)
    } catch (e: any) {
        balanceError.value = e?.message ?? 'Failed to load balance'
        throw e
    } finally {
        loadingBalance.value = false
    }
}
