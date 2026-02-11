import {ref} from 'vue'
import {api} from './useApi'
import {accessToken} from './useAuth'
import type {Transaction, TransactionHistoryResponse} from '../types'

const transactions = ref<Transaction[]>([])
const loadingTransactions = ref(false)
const transactionsError = ref<string | null>(null)

export function useTransactions() {
    /**
     * Load user transaction history from backend
     */
    const loadTransactions = async () => {
        loadingTransactions.value = true
        transactionsError.value = null

        try {
            const headers: Record<string, string> = {}
            if (accessToken.value) {
                headers.Authorization = `Bearer ${accessToken.value}`
            }

            const response = await api.get<TransactionHistoryResponse>('/wallet/transactions', {headers})
            transactions.value = response.data.transactions || []
        } catch (err: any) {
            transactionsError.value = err.response?.data?.message || err.message || 'Failed to load transactions'
            console.error('Error loading transactions:', err)
        } finally {
            loadingTransactions.value = false
        }
    }

    /**
     * Refresh transactions
     */
    const refreshTransactions = async () => {
        await loadTransactions()
    }

    /**
     * Format amount from nano to readable format
     */
    const formatAmount = (amountNano: string | number, decimals: number = 9): string => {
        const amount = Number(amountNano) / Math.pow(10, decimals)
        if (decimals === 9) {
            // TON - show 4 decimals
            return amount.toFixed(4)
        } else {
            // Jetton - show 2 decimals
            return amount.toFixed(2)
        }
    }

    /**
     * Format address to short form
     */
    const formatAddress = (address?: string): string => {
        if (!address) return 'Unknown'
        if (address.length <= 12) return address
        return `${address.substring(0, 6)}...${address.substring(address.length - 6)}`
    }

    /**
     * Get TON Viewer URL for transaction
     */
    const getTonViewerUrl = (hash: string): string => {
        return `https://tonviewer.com/transaction/${hash}`
    }

    /**
     * Format date to readable format
     */
    const formatDate = (dateString: string): string => {
        const date = new Date(dateString)
        return date.toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    return {
        transactions,
        loadingTransactions,
        transactionsError,
        loadTransactions,
        refreshTransactions,
        formatAmount,
        formatAddress,
        getTonViewerUrl,
        formatDate
    }
}
