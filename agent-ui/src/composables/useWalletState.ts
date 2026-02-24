import {computed, ref} from 'vue'
import {api} from './useApi.ts'
import {accessToken} from './useAuth'
import type {AssetData, SwapData, WalletStateResponse} from '../types'

const TONAPI_BASE_URL = 'https://tonapi.io/v2'

// State
const walletState = ref<WalletStateResponse | null>(null)
const loadingWalletState = ref(false)
const walletStateError = ref<string | null>(null)

// Jetton metadata cache for image enrichment
const jettonMetadataCache = new Map<string, { image: string }>()

/**
 * Fetch jetton metadata from TonAPI (only for images)
 */
async function fetchJettonImage(address: string): Promise<string | null> {
    if (jettonMetadataCache.has(address)) {
        return jettonMetadataCache.get(address)!.image
    }

    try {
        const response = await fetch(`${TONAPI_BASE_URL}/jettons/${address}`)
        if (!response.ok) return null

        const data = await response.json()
        const image = data.metadata?.image || data.preview || ''

        jettonMetadataCache.set(address, {image})
        return image
    } catch (error) {
        console.error(`Error fetching jetton metadata for ${address}:`, error)
        return null
    }
}

/**
 * Enrich assets with images from TonAPI
 */
async function enrichAssetsWithImages(assets: AssetData[]): Promise<AssetData[]> {
    return await Promise.all(
        assets.map(async (asset) => {
            // TON native token
            if (asset.address === 'TON' || asset.symbol === 'TON') {
                return {
                    ...asset,
                    imageUrl: 'https://assets.coingecko.com/coins/images/17980/small/ton_symbol.png'
                }
            }

            // For jettons, only fetch image if not provided by backend
            if (!asset.imageUrl) {
                const image = await fetchJettonImage(asset.address)
                if (image) {
                    return {...asset, imageUrl: image}
                }
            }

            return asset
        })
    )
}

/**
 * Computed values for backward compatibility
 */
export const balanceUsd = computed(() => walletState.value?.balance.totalUsd ?? null)
export const assets = computed(() => walletState.value?.assets ?? [])
export const transactions = computed(() => walletState.value?.transactions ?? [])
export const swaps = computed<SwapData[]>(() => walletState.value?.swaps ?? [])
export const orders = computed(() => walletState.value?.orders ?? [])
export const metadata = computed(() => walletState.value?.metadata ?? null)

// Sorted assets (TON first, then by USD value)
export const sortedAssets = computed(() => {
    return [...assets.value].sort((a, b) => {
        if (a.address === 'TON') return -1
        if (b.address === 'TON') return 1

        if (a.usdValue && b.usdValue) {
            return b.usdValue - a.usdValue
        }

        return BigInt(b.amountNano) > BigInt(a.amountNano) ? 1 : -1
    })
})

/**
 * Clear wallet state (e.g., on logout)
 */
export function clearWalletState() {
    walletState.value = null
    loadingWalletState.value = false
    walletStateError.value = null
    jettonMetadataCache.clear()
}

export function useWalletState() {
    /**
     * Load unified wallet state from backend
     */
    const loadWalletState = async (userId: number, transactionsLimit: number = 20) => {
        loadingWalletState.value = true
        walletStateError.value = null

        try {
            const headers: Record<string, string> = {}
            if (accessToken.value) {
                headers.Authorization = `Bearer ${accessToken.value}`
            }

            const response = await api.get<WalletStateResponse>(
                `/user/${userId}/wallet-state?transactionsLimit=${transactionsLimit}`,
                {headers}
            )

            // Enrich assets with images
            const enrichedAssets = await enrichAssetsWithImages(response.data.assets)

            walletState.value = {
                ...response.data,
                assets: enrichedAssets
            }
        } catch (err: any) {
            walletStateError.value = err.response?.data?.message || err.message || 'Failed to load wallet state'
            console.error('Error loading wallet state:', err)
            // Don't throw - just set error state and let UI handle it
        } finally {
            loadingWalletState.value = false
        }
    }

    /**
     * Refresh wallet state
     */
    const refreshWalletState = async (userId: number, transactionsLimit: number = 20) => {
        jettonMetadataCache.clear()
        await loadWalletState(userId, transactionsLimit)
    }

    return {
        walletState,
        loadingWalletState,
        walletStateError,
        balanceUsd,
        assets: sortedAssets,
        transactions,
        swaps,
        orders,
        metadata,
        loadWalletState,
        refreshWalletState,
        clearWalletState
    }
}
