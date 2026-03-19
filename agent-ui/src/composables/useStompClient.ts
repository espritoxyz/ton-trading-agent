/**
 * Shared STOMP/WebSocket connection for the entire app.
 * Both useNotifications and useChat plug into this single connection.
 */
import { ref, computed } from 'vue'
import { Client, type StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { ChatUpdateEvent } from '../types'
import { accessToken, userId, refreshAccessToken } from './useAuth'

// ── Connection state ────────────────────────────────────────────────────────
const stompClient = ref<Client | null>(null)
const _connected = ref(false)

// ── Generic topic registry ──────────────────────────────────────────────────
// Any module can register a topic handler; it is auto-subscribed on (re)connect.
type TopicEntry = { handler: (body: string) => void; sub: StompSubscription | null }
const topicRegistry = new Map<string, TopicEntry>()

export function registerTopic(topic: string, handler: (body: string) => void): () => void {
    const entry: TopicEntry = { handler, sub: null }
    topicRegistry.set(topic, entry)
    // Subscribe immediately if the client is already up
    if (stompClient.value?.connected) {
        entry.sub = stompClient.value.subscribe(topic, (msg) => entry.handler(msg.body))
        console.log('[WS] Subscribed to:', topic)
    }
    return () => {
        entry.sub?.unsubscribe()
        topicRegistry.delete(topic)
    }
}

function subscribeAll() {
    if (!stompClient.value?.connected || !userId.value) return
    for (const [topic, entry] of topicRegistry) {
        if (!entry.sub) {
            entry.sub = stompClient.value.subscribe(topic, (msg) => entry.handler(msg.body))
            console.log('[WS] Subscribed to:', topic)
        }
    }
    subscribeChatTopic()
}

function clearSubscriptions() {
    // Subscriptions are invalid once the client disconnects — just null them out
    for (const entry of topicRegistry.values()) entry.sub = null
    chatTopicSub = null
}

// ── Chat update topic ───────────────────────────────────────────────────────
// Handled here (not in useNotifications) because it is WS infrastructure, not notification logic.
type ChatUpdateCallback = (event: ChatUpdateEvent) => void
const chatUpdateListeners = new Set<ChatUpdateCallback>()
export const chatUpdateCache = new Map<string, ChatUpdateEvent>()
let chatTopicSub: StompSubscription | null = null

export function onChatUpdate(cb: ChatUpdateCallback): () => void {
    chatUpdateListeners.add(cb)
    return () => chatUpdateListeners.delete(cb)
}

function subscribeChatTopic() {
    if (!stompClient.value?.connected || !userId.value || chatTopicSub) return
    const topic = `/topic/chat/${userId.value}`
    console.log('[WS] Subscribing to:', topic)
    chatTopicSub = stompClient.value.subscribe(topic, (msg) => {
        try {
            const event: ChatUpdateEvent = JSON.parse(msg.body)
            console.debug('[WS] Chat update:', event.messageId, event.status)
            chatUpdateCache.set(event.messageId, event)
            chatUpdateListeners.forEach(cb => cb(event))
        } catch (e) {
            console.error('[WS] Failed to parse chat update:', e)
        }
    })
}

// ── Reconnect & health check ────────────────────────────────────────────────
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000]
const HEALTH_CHECK_INTERVAL = 30_000

let reconnectAttempt = 0
let reconnectTimer: number | null = null
let healthCheckTimer: number | null = null
let isConnecting = false
let isNetworkOnline = navigator.onLine

function scheduleReconnect() {
    if (reconnectTimer) return
    const delay = RECONNECT_DELAYS[Math.min(reconnectAttempt, RECONNECT_DELAYS.length - 1)]
    console.log(`[WS] Reconnecting in ${delay}ms (attempt ${reconnectAttempt + 1})`)
    reconnectTimer = window.setTimeout(() => {
        reconnectTimer = null
        reconnectAttempt++
        connect()
    }, delay)
}

function startHealthCheck() {
    stopHealthCheck()
    healthCheckTimer = window.setInterval(() => {
        if (!accessToken.value || !userId.value) return
        if (stompClient.value?.connected) return
        if (reconnectTimer || isConnecting) return
        console.warn('[WS] Health check: connection lost, reconnecting')
        scheduleReconnect()
    }, HEALTH_CHECK_INTERVAL)
}

function stopHealthCheck() {
    if (healthCheckTimer) { clearInterval(healthCheckTimer); healthCheckTimer = null }
}

// ── Connect / disconnect ────────────────────────────────────────────────────
function connect() {
    if (stompClient.value?.active) { console.log('[WS] Already connected'); return }
    if (!accessToken.value || !userId.value) { console.warn('[WS] Cannot connect: no token or userId'); return }

    const wsUrl = (import.meta.env.VITE_API_BASE_URL || '') + '/ws'
    console.log('[WS] Connecting to:', wsUrl)

    const client = new Client({
        webSocketFactory: () => new SockJS(wsUrl) as any,
        connectHeaders: { Authorization: `Bearer ${accessToken.value}` },
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        reconnectDelay: 0,
        onConnect: () => {
            console.log('[WS] Connected')
            _connected.value = true
            reconnectAttempt = 0
            subscribeAll()
        },
        onDisconnect: () => {
            console.log('[WS] Disconnected')
            _connected.value = false
            clearSubscriptions()
            if (stompClient.value && isNetworkOnline) scheduleReconnect()
        },
        onStompError: (frame) => {
            console.error('[WS] STOMP error:', frame)
            _connected.value = false
            scheduleReconnect()
        },
        onWebSocketError: (error) => {
            console.error('[WS] WebSocket error:', error)
            _connected.value = false
            scheduleReconnect()
        }
    })

    stompClient.value = client
    client.activate()
}

function disconnect() {
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
    clearSubscriptions()
    if (stompClient.value) {
        const client = stompClient.value
        stompClient.value = null // null before deactivate so onDisconnect won't trigger reconnect
        client.deactivate()
    }
    stopHealthCheck()
    _connected.value = false
    reconnectAttempt = 0
}

async function connectWithFreshToken(resetBackoff = false) {
    if (isConnecting) return
    if (!accessToken.value || !userId.value) return
    isConnecting = true
    try {
        if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null }
        if (resetBackoff) reconnectAttempt = 0
        const refreshed = await refreshAccessToken()
        if (!refreshed) return
        if (stompClient.value?.active) {
            const old = stompClient.value
            stompClient.value = null
            await old.deactivate()
        }
        connect()
    } finally {
        isConnecting = false
    }
}

// ── Visibility / online listeners (ref-counted) ─────────────────────────────
let listenerRefCount = 0
let handleVisibilityChange: (() => void) | null = null
let handleOnline: (() => void) | null = null
let handleOffline: (() => void) | null = null
let handlePageShow: ((e: PageTransitionEvent) => void) | null = null

function initListeners() {
    listenerRefCount++
    if (listenerRefCount > 1) return

    handleVisibilityChange = async () => {
        if (document.visibilityState !== 'visible') return
        if (!accessToken.value || !userId.value) return
        if (!stompClient.value?.connected) await connectWithFreshToken(true)
    }
    handleOnline = async () => {
        isNetworkOnline = true
        if (!accessToken.value || !userId.value) return
        if (!stompClient.value?.connected) await connectWithFreshToken(true)
    }
    handleOffline = () => { isNetworkOnline = false }
    handlePageShow = async (e: PageTransitionEvent) => {
        if (!e.persisted) return
        if (!accessToken.value || !userId.value) return
        await connectWithFreshToken(true)
    }

    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('online', handleOnline)
    window.addEventListener('offline', handleOffline)
    window.addEventListener('pageshow', handlePageShow)
    startHealthCheck()
}

function destroyListeners() {
    listenerRefCount = Math.max(0, listenerRefCount - 1)
    if (listenerRefCount > 0) return
    if (handleVisibilityChange) { document.removeEventListener('visibilitychange', handleVisibilityChange); handleVisibilityChange = null }
    if (handleOnline) { window.removeEventListener('online', handleOnline); handleOnline = null }
    if (handleOffline) { window.removeEventListener('offline', handleOffline); handleOffline = null }
    if (handlePageShow) { window.removeEventListener('pageshow', handlePageShow); handlePageShow = null }
    stopHealthCheck()
}

// ── Composable ──────────────────────────────────────────────────────────────
export function useStompClient() {
    return {
        connected: computed(() => _connected.value),
        connect,
        disconnect,
        initListeners,
        destroyListeners,
    }
}
