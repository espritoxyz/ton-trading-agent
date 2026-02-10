import {computed, ref} from 'vue'
import {api} from './useApi'
import {accessToken} from './useAuth'
import type {Asset, JettonMetadata} from '../types'

const assets = ref<Asset[]>([])
const loadingAssets = ref(false)
const assetsError = ref<string | null>(null)

// Cache for jetton metadata to avoid repeated API calls
const jettonMetadataCache = new Map<string, JettonMetadata>()

const TONAPI_BASE_URL = 'https://tonapi.io/v2'

/**
 * Fetch jetton metadata from TonAPI
 */
async function fetchJettonMetadata(address: string): Promise<JettonMetadata | null> {
    // Check cache first
    if (jettonMetadataCache.has(address)) {
        return jettonMetadataCache.get(address)!
    }

    try {
        const response = await fetch(`${TONAPI_BASE_URL}/jettons/${address}`)
        if (!response.ok) {
            console.warn(`Failed to fetch jetton metadata for ${address}:`, response.statusText)
            return null
        }

        const data = await response.json()

        const metadata: JettonMetadata = {
            address: data.address || address,
            name: data.metadata?.name || 'Unknown',
            symbol: data.metadata?.symbol || '???',
            decimals: data.metadata?.decimals || 9,
            image: data.metadata?.image || data.preview || '',
            verification: data.verification
        }

        // Cache the result
        jettonMetadataCache.set(address, metadata)
        return metadata
    } catch (error) {
        console.error(`Error fetching jetton metadata for ${address}:`, error)
        return null
    }
}

/**
 * Enrich assets with jetton metadata from TonAPI
 * Backend already provides symbol, decimals, readableAmount, unitPrice, usdValue
 * We only need to fetch imageUrl from TonAPI
 */
async function enrichAssetsWithMetadata(rawAssets: Asset[]): Promise<Asset[]> {
    return await Promise.all(
        rawAssets.map(async (asset) => {
            // TON native token
            if (asset.address === 'TON' || asset.symbol === 'TON') {
                return {
                    ...asset,
                    symbol: asset.symbol || 'TON',
                    decimals: asset.decimals || 9,
                    name: asset.name || 'Toncoin',
                    imageUrl: 'https://assets.coingecko.com/coins/images/17980/small/ton_symbol.png'
                }
            }

            // For jettons, only fetch image if not provided by backend
            if (!asset.imageUrl) {
                const metadata = await fetchJettonMetadata(asset.address)
                if (metadata) {
                    return {
                        ...asset,
                        imageUrl: metadata.image
                    }
                }
            }

            // Return asset as-is (backend already provided symbol, decimals, etc.)
            return asset
        })
    )
}

/**
 * Computed: sorted assets (TON first, then by USD value if available, then by amount)
 * Backend already provides readableAmount, so no need to calculate it
 */
const sortedAssets = computed(() => {
    return [...assets.value].sort((a, b) => {
        // TON always first
        if (a.address === 'TON') return -1
        if (b.address === 'TON') return 1

        // Sort by USD value if available
        if (a.usdValue && b.usdValue) {
            return b.usdValue - a.usdValue
        }

        // Otherwise sort by amount
        return BigInt(b.amountNano) > BigInt(a.amountNano) ? 1 : -1
    })
})

export function useAssets() {
    /**
     * Load user assets from backend and enrich with metadata
     */
    const loadAssets = async (userId: number) => {
        loadingAssets.value = true
        assetsError.value = null

        try {
            const headers: Record<string, string> = {}
            if (accessToken.value) {
                headers.Authorization = `Bearer ${accessToken.value}`
            }

            const response = await api.get(`/user/${userId}/assets`, {headers})
            const rawAssets: Asset[] = response.data

            // Enrich with metadata from TonAPI
            assets.value = await enrichAssetsWithMetadata(rawAssets)
        } catch (err: any) {
            assetsError.value = err.response?.data?.message || err.message || 'Failed to load assets'
            console.error('Error loading assets:', err)
        } finally {
            loadingAssets.value = false
        }
    }

    /**
     * Refresh assets (clear cache and reload)
     */
    const refreshAssets = async (userId: number) => {
        jettonMetadataCache.clear()
        await loadAssets(userId)
    }

    return {
        assets: sortedAssets,
        loadingAssets,
        assetsError,
        loadAssets,
        refreshAssets
    }
}
