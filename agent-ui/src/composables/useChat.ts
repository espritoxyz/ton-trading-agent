import { ref, watch, onUnmounted } from 'vue'
import { api } from './useApi.ts'
import { onChatUpdate, chatUpdateCache } from './useStompClient.ts'
import type { ChatItem, ChatRole, ChatUpdateEvent } from '../types.ts'

type PostResp = {
    messageId: string
    userId: number
    status: 'queued' | 'processing' | 'completed' | 'error' | 'toolcalling'
    echo: string
    reply: string | null
    queuedAt: string
    completedAt?: string | null
    delivery?: { mode: 'poll' | 'sse' | 'websocket'; resultUrl?: string | null }
}

type BackendChatMessage = {
    type: ChatRole
    content: string
}

const STORAGE_KEY_BASE = 'ton-agent-chat'
const HISTORY_LIMIT = 20

function buildStorageKey(userId?: number) {
    return userId ? `${STORAGE_KEY_BASE}:${userId}` : STORAGE_KEY_BASE
}

function loadMessages(storageKey: string): ChatItem[] {
    if (typeof window === 'undefined') return []
    try {
        const raw = localStorage.getItem(storageKey)
        if (!raw) return []
        const parsed = JSON.parse(raw) as ChatItem[]
        return Array.isArray(parsed) ? parsed : []
    } catch {
        return []
    }
}

export function useChat(userId?: number) {
    const storageKey = buildStorageKey(userId)
    const messages = ref<ChatItem[]>(loadMessages(storageKey))
    const sending = ref(false)

    // messageId of the currently in-flight request (null when idle)
    const activeSendingMessageId = ref<string | null>(null)

    watch(messages, (val) => {
        if (typeof window === 'undefined') return
        localStorage.setItem(storageKey, JSON.stringify(val))
    }, { deep: true })

    function push(role: ChatRole, content: string, backendMessageId?: string) {
        messages.value.push({
            id: `${role}_${Date.now()}_${Math.random().toString(16).slice(2)}`,
            role,
            content,
            backendMessageId,
            createdAt: new Date().toISOString()
        })
    }

    function buildHistory(): BackendChatMessage[] {
        const all = messages.value
        const slice = all.length <= HISTORY_LIMIT ? all : all.slice(all.length - HISTORY_LIMIT)
        return slice.map((m) => ({ type: m.role, content: m.content }))
    }

    function updateSystemMessage(messageId: string, newContent: string) {
        const msg = messages.value.slice().reverse().find((m) => m.backendMessageId === messageId)
        if (!msg) { console.warn(`[Chat] Cannot find bubble for messageId=${messageId}`); return }
        msg.content = newContent
    }

    function removeConfirmationBubbles(messageId: string) {
        messages.value = messages.value.filter(
            (m) => !m.utilityKind || m.utilityMeta?.messageId !== messageId
        )
    }

    /** Apply a WS event to local state. Returns true if the status is terminal. */
    function applyUpdate(event: ChatUpdateEvent): boolean {
        const { messageId, status, reply, confirmations } = event

        if (status === 'completed') {
            removeConfirmationBubbles(messageId)
            updateSystemMessage(messageId, reply || '…')
            return true
        }

        if (status === 'error') {
            removeConfirmationBubbles(messageId)
            updateSystemMessage(messageId, reply || 'Error processing request.')
            return true
        }

        if (status === 'toolcalling' && confirmations?.length) {
            updateSystemMessage(messageId, 'Waiting for confirmations...')
            for (const c of confirmations) {
                const exists = messages.value.some((m) => m.backendMessageId === c.id)
                if (!exists && c.status === 'PENDING') {
                    const utilityKind = c.toolName === 'show_top_up_dialog'
                        ? 'SHOW_TOP_UP' as const
                        : 'CONFIRM_SEND_TON' as const
                    messages.value.push({
                        id: `CONFIRM_${c.id}`,
                        role: 'SYSTEM',
                        content: c.text,
                        backendMessageId: c.id,
                        createdAt: new Date().toISOString(),
                        utilityKind,
                        utilityMeta: { messageId, confirmationId: c.id }
                    })
                }
            }
        }

        if (status === 'processing') {
            updateSystemMessage(messageId, 'Processing...')
        }

        return false
    }

    const stopChatListener = onChatUpdate((event: ChatUpdateEvent) => {
        // Always apply — even if this message wasn't sent from this tab
        const terminal = applyUpdate(event)
        if (activeSendingMessageId.value === event.messageId && terminal) {
            sending.value = false
            activeSendingMessageId.value = null
        }
    })
    onUnmounted(() => stopChatListener())

    // On init: replay any cached events for bubbles that are still "Thinking…"
    // This handles the case where the user switched tabs while waiting
    for (const [msgId, event] of chatUpdateCache) {
        const hasStale = messages.value.some((m) => m.backendMessageId === msgId)
        if (hasStale) applyUpdate(event)
    }

    async function sendMessage(text: string) {
        const trimmed = text.trim()
        if (!trimmed) return

        const history = buildHistory()
        push('USER', trimmed)
        sending.value = true

        try {
            const { data } = await api.post<PostResp>('/chat/message', {
                content: trimmed,
                history
            })

            const messageId = data.messageId

            // Fast path: backend already completed (shouldn't happen, but handle it)
            if (data.status === 'completed' && data.reply) {
                push('SYSTEM', data.reply, messageId)
                sending.value = false
                return
            }

            push('SYSTEM', 'Thinking…', messageId)
            activeSendingMessageId.value = messageId

            // WS event may have arrived before we got here — check cache
            const cached = chatUpdateCache.get(messageId)
            if (cached) {
                const terminal = applyUpdate(cached)
                if (terminal) {
                    sending.value = false
                    activeSendingMessageId.value = null
                }
            }
            // Otherwise the onChatUpdate listener above will handle it
        } catch {
            sending.value = false
            activeSendingMessageId.value = null
        }
    }

    function clearChat() {
        messages.value = []
        if (typeof window !== 'undefined') {
            localStorage.removeItem(storageKey)
        }
    }

    return { messages, sending, sendMessage, clearChat } as const
}
