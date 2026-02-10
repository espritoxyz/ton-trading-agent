import { ref, watch } from 'vue'
import { api } from './useApi.ts'
import type {ChatItem, ChatRole} from '../types.ts'

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

type StatusResp = {
    messageId: string
    userId: number
    status: 'queued' | 'processing' | 'completed' | 'error' | 'toolcalling'
    reply: string | null
    queuedAt: string
    completedAt?: string | null
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

    watch(
        messages,
        (val) => {
            if (typeof window === 'undefined') return
            localStorage.setItem(storageKey, JSON.stringify(val))
        },
        { deep: true }
    )

    function push(
        role: ChatRole,
        content: string,
        backendMessageId?: string
    ) {
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
        const slice =
            all.length <= HISTORY_LIMIT ? all : all.slice(all.length - HISTORY_LIMIT)
        return slice.map((m) => ({
            type: m.role,
            content: m.content
        }))
    }

    function updateSystemMessage(messageId: string, newContent: string) {
        console.debug(`Updating message ${messageId} with ${newContent}`)
        const idx = messages.value
            .slice()
            .reverse()
            .find((m) => m.backendMessageId === messageId)
        if (!idx) {
            console.warn(`Could not locate message ${messageId}`)
            return
        }
        idx.content = newContent
    }

    async function sendMessage(text: string) {
        const trimmed = text.trim()
        if (!trimmed) return

        const history = buildHistory()

        // optimistic user bubble
        push('USER', trimmed)
        sending.value = true

        try {
            const payload = {
                content: trimmed,
                history
            }

            const { data } = await api.post<PostResp>('/chat/message', payload)

            const messageId = data.messageId

            // placeholder system bubble linked to messageId
            push('SYSTEM', 'Thinking…', messageId)

            // if backend already completed synchronously (unlikely, but possible)
            if (data.status === 'completed' && data.reply) {
                updateSystemMessage(messageId, data.reply)
                return
            }

            let requestedConfirmations = false;

            // queued/processing: poll
            let delay = 600
            for (let i = 0; i < 20; i++) {
                await new Promise((r) => setTimeout(r, delay))
                const status = await poll(messageId)
                console.debug(`Current message status: ${status.status}=${status.reply}`)
                if (status.status === 'completed' && status.reply) {
                    updateSystemMessage(messageId, status.reply)
                    return
                }
                if (status.status === 'error') {
                    updateSystemMessage(messageId, status.reply || 'Error processing request.')
                    return
                }
                if (status.status === 'toolcalling') {
                    updateSystemMessage(messageId, 'Calling AI tools...')
                    // Do NOT return; continue to fetch confirmations below
                }
                // fetch pending confirmations and render them as utility bubbles
                const confs = await listConfirmations(messageId)
                console.debug('[useChat] confirmations for', messageId, confs)

                if (confs.length > 0 && confs.some(conf => conf.status !== 'APPROVED')) {
                    updateSystemMessage(messageId, 'Waiting for confirmations...')
                    requestedConfirmations = true
                } else if (requestedConfirmations) {
                    updateSystemMessage(messageId, "Processing...")
                    requestedConfirmations = false
                }

                for (const c of confs) {
                    const exists = messages.value.some(m => m.backendMessageId === c.id)
                    if (!exists && c.status === 'PENDING') {
                        // Map toolName to utilityKind
                        let utilityKind: 'CONFIRM_SEND_TON' | 'SHOW_TOP_UP' = 'CONFIRM_SEND_TON'
                        if (c.toolName === 'show_top_up_dialog') {
                            utilityKind = 'SHOW_TOP_UP'
                        } else if (c.toolName === 'send_ton_to_address') {
                            utilityKind = 'CONFIRM_SEND_TON'
                        }

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
                delay = Math.min(delay * 1.6, 4000)
            }

            updateSystemMessage(
                messageId,
                'Still processing… please try again in a moment.'
            )
        } finally {
            sending.value = false
        }
    }

    async function poll(messageId: string) {
        const { data } = await api.get<StatusResp>(`/chat/messages/${messageId}`)
        return data
    }

    async function listConfirmations(messageId: string) {
        const { data } = await api.get<Array<{ id: string; toolName: string; text: string; status: 'PENDING' | 'APPROVED' | 'DECLINED' }>>(`/chat/messages/${messageId}/confirmations`)
        return data
    }

    function clearChat() {
        messages.value = []
        if (typeof window !== 'undefined') {
            localStorage.removeItem(storageKey)
        }
    }
    
    return { messages, sending, sendMessage, clearChat } as const
}
