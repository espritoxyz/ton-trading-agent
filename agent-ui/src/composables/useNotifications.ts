import { ref, computed } from 'vue'
import { Client, StompSubscription } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import type { Notification } from '../types'
import { accessToken, userId } from './useAuth'
import { api } from './useApi'

const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const connected = ref(false)
const stompClient = ref<Client | null>(null)
let subscription: StompSubscription | null = null
let reconnectAttempt = 0
let reconnectTimer: number | null = null

const MAX_RECONNECT_DELAY = 30000 // 30 seconds
const RECONNECT_DELAYS = [1000, 2000, 4000, 8000, 16000, 30000] // Exponential backoff

export function useNotifications() {

    /**
     * Connect to WebSocket and subscribe to notifications
     */
    function connect() {
        if (stompClient.value?.active) {
            console.log('[Notifications] Already connected')
            return
        }

        const token = accessToken.value
        if (!token) {
            console.warn('[Notifications] Cannot connect: no access token')
            return
        }

        if (!userId.value) {
            console.warn('[Notifications] Cannot connect: no user ID')
            return
        }

        const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
        const wsUrl = `${baseUrl}/ws`

        console.log('[Notifications] Connecting to WebSocket at:', wsUrl)

        const client = new Client({
            webSocketFactory: () => new SockJS(wsUrl) as any,
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            reconnectDelay: 0, // We handle reconnection manually
            onConnect: () => {
                console.log('[Notifications] WebSocket connected')
                connected.value = true
                reconnectAttempt = 0

                // Subscribe to user-specific notification topic
                subscribe()
            },
            onDisconnect: () => {
                console.log('[Notifications] WebSocket disconnected')
                connected.value = false
                subscription = null
            },
            onStompError: (frame) => {
                console.error('[Notifications] STOMP error:', frame)
                connected.value = false

                // Attempt reconnection
                scheduleReconnect()
            },
            onWebSocketError: (error) => {
                console.error('[Notifications] WebSocket error:', error)
                connected.value = false

                // Attempt reconnection
                scheduleReconnect()
            }
        })

        stompClient.value = client
        client.activate()
    }

    /**
     * Disconnect from WebSocket
     */
    function disconnect() {
        if (reconnectTimer) {
            clearTimeout(reconnectTimer)
            reconnectTimer = null
        }

        if (subscription) {
            subscription.unsubscribe()
            subscription = null
        }

        if (stompClient.value) {
            stompClient.value.deactivate()
            stompClient.value = null
        }

        connected.value = false
        reconnectAttempt = 0
    }

    /**
     * Subscribe to user-specific notification topic
     */
    function subscribe() {
        if (!stompClient.value?.connected || !userId.value) {
            console.warn('[Notifications] Cannot subscribe: client not connected or no user ID')
            return
        }

        const topic = `/topic/notifications/${userId.value}`
        console.log('[Notifications] Subscribing to:', topic)

        subscription = stompClient.value.subscribe(topic, (message) => {
            try {
                const notification: Notification = JSON.parse(message.body)
                handleIncoming(notification)
            } catch (e) {
                console.error('[Notifications] Failed to parse notification:', e)
            }
        })
    }

    /**
     * Schedule reconnection with exponential backoff
     */
    function scheduleReconnect() {
        if (reconnectTimer) {
            return // Already scheduled
        }

        const delay = RECONNECT_DELAYS[Math.min(reconnectAttempt, RECONNECT_DELAYS.length - 1)]
        console.log(`[Notifications] Reconnecting in ${delay}ms (attempt ${reconnectAttempt + 1})`)

        reconnectTimer = window.setTimeout(() => {
            reconnectTimer = null
            reconnectAttempt++
            connect()
        }, delay)
    }

    /**
     * Handle incoming notification from WebSocket
     */
    function handleIncoming(notification: Notification) {
        console.log('[Notifications] Received notification:', notification)

        // Deduplicate by ID
        const exists = notifications.value.some(n => n.id === notification.id)
        if (exists) {
            console.log('[Notifications] Notification already exists, skipping')
            return
        }

        // Add to beginning of list (newest first)
        notifications.value.unshift(notification)

        // Update unread count if not read
        if (!notification.isRead) {
            unreadCount.value++
        }

        // Trigger browser notification if permission granted
        triggerBrowserNotification(notification)
    }

    /**
     * Trigger browser notification
     */
    function triggerBrowserNotification(notification: Notification) {
        if (typeof Notification === 'undefined') {
            return // Browser doesn't support notifications
        }

        if (Notification.permission === 'granted') {
            try {
                const browserNotif = new Notification(notification.title, {
                    body: notification.message,
                    icon: '/favicon.ico',
                    tag: `notification-${notification.id}`,
                    requireInteraction: false
                })

                // Auto-close after 5 seconds
                setTimeout(() => browserNotif.close(), 5000)
            } catch (e) {
                console.error('[Notifications] Failed to show browser notification:', e)
            }
        }
    }

    /**
     * Request browser notification permission
     */
    async function requestNotificationPermission(): Promise<NotificationPermission | null> {
        if (typeof Notification === 'undefined') {
            console.warn('[Notifications] Browser does not support notifications')
            return null
        }

        if (Notification.permission === 'granted') {
            return 'granted'
        }

        if (Notification.permission === 'denied') {
            return 'denied'
        }

        try {
            const permission = await Notification.requestPermission()
            return permission
        } catch (e) {
            console.error('[Notifications] Failed to request notification permission:', e)
            return null
        }
    }

    /**
     * Fetch notifications from REST API
     */
    async function fetchNotifications(page = 0, size = 50, unreadOnly = false) {
        try {
            const params: any = { page, size }
            if (unreadOnly) {
                params.unread = true
            }

            const response = await api.get('/api/notifications', { params })
            const data = response.data

            if (Array.isArray(data.content)) {
                // Deduplicate with existing notifications
                const newNotifications = data.content.filter(
                    (n: Notification) => !notifications.value.some(existing => existing.id === n.id)
                )

                // Add to list
                notifications.value.push(...newNotifications)

                // Sort by createdAt descending
                notifications.value.sort((a, b) =>
                    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
                )
            }
        } catch (e) {
            console.error('[Notifications] Failed to fetch notifications:', e)
        }
    }

    /**
     * Fetch unread count from API
     */
    async function fetchUnreadCount() {
        try {
            const response = await api.get('/api/notifications/unread-count')
            unreadCount.value = response.data.count || 0
        } catch (e) {
            console.error('[Notifications] Failed to fetch unread count:', e)
        }
    }

    /**
     * Mark notification as read
     */
    async function markAsRead(id: number) {
        try {
            await api.patch(`/api/notifications/${id}/read`)

            // Update local state
            const notification = notifications.value.find(n => n.id === id)
            if (notification && !notification.isRead) {
                notification.isRead = true
                notification.readAt = new Date().toISOString()
                unreadCount.value = Math.max(0, unreadCount.value - 1)
            }
        } catch (e) {
            console.error('[Notifications] Failed to mark notification as read:', e)
            throw e
        }
    }

    /**
     * Mark all notifications as read
     */
    async function markAllAsRead() {
        try {
            const unreadNotifications = notifications.value.filter(n => !n.isRead)

            // Mark each unread notification as read
            await Promise.all(
                unreadNotifications.map(n => markAsRead(n.id))
            )
        } catch (e) {
            console.error('[Notifications] Failed to mark all as read:', e)
        }
    }

    /**
     * Delete notification
     */
    async function deleteNotification(id: number) {
        try {
            await api.delete(`/api/notifications/${id}`)

            // Remove from local state
            const index = notifications.value.findIndex(n => n.id === id)
            if (index !== -1) {
                const notification = notifications.value[index]
                if (!notification.isRead) {
                    unreadCount.value = Math.max(0, unreadCount.value - 1)
                }
                notifications.value.splice(index, 1)
            }
        } catch (e) {
            console.error('[Notifications] Failed to delete notification:', e)
            throw e
        }
    }

    /**
     * Delete all notifications
     */
    async function deleteAllNotifications() {
        try {
            const response = await api.delete('/api/notifications')
            const deletedCount = response.data.deletedCount || 0

            console.log(`[Notifications] Deleted ${deletedCount} notifications`)

            // Clear local state
            const unreadCountBeforeClear = notifications.value.filter(n => !n.isRead).length
            notifications.value = []
            unreadCount.value = 0

            return deletedCount
        } catch (e) {
            console.error('[Notifications] Failed to delete all notifications:', e)
            throw e
        }
    }

    const notificationPermission = computed(() => {
        if (typeof Notification === 'undefined') {
            return 'unsupported' as const
        }
        return Notification.permission
    })

    return {
        // State
        notifications: computed(() => notifications.value),
        unreadCount: computed(() => unreadCount.value),
        connected: computed(() => connected.value),
        notificationPermission,

        // Actions
        connect,
        disconnect,
        fetchNotifications,
        fetchUnreadCount,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        deleteAllNotifications,
        requestNotificationPermission
    }
}
