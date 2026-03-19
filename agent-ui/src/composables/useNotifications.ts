import { ref, computed } from 'vue'
import type { Notification } from '../types'
import { userId } from './useAuth'
import { api } from './useApi.ts'
import { registerTopic, useStompClient } from './useStompClient.ts'

const notifications = ref<Notification[]>([])
const unreadCount = ref(0)

// Exposed so Dashboard can watch for data-changing notifications and refresh wallet
export const lastIncomingNotification = ref<Notification | null>(null)

// Per-instance topic unregister handle (supports logout/login cycles)
let unregisterNotifTopic: (() => void) | null = null

function handleIncoming(notification: Notification) {
    console.log('[Notifications] Received:', notification)

    // Refresh-only signals: trigger data reload but do not display as a notification
    if (notification.refreshOnly) {
        lastIncomingNotification.value = notification
        return
    }

    // Deduplicate by ID
    if (notifications.value.some(n => n.id === notification.id)) {
        console.log('[Notifications] Already exists, skipping')
        return
    }

    notifications.value.unshift(notification) // newest first
    if (!notification.isRead) unreadCount.value++
    lastIncomingNotification.value = notification
    triggerBrowserNotification(notification)
}

function triggerBrowserNotification(notification: Notification) {
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return
    try {
        const n = new Notification(notification.title, {
            body: notification.message,
            icon: '/favicon.ico',
            tag: `notification-${notification.id}`,
            requireInteraction: false
        })
        setTimeout(() => n.close(), 5000)
    } catch (e) {
        console.error('[Notifications] Failed to show browser notification:', e)
    }
}

export function useNotifications() {
    const { connected, connect: wsConnect, disconnect: wsDisconnect, initListeners, destroyListeners } = useStompClient()

    function connect() {
        if (!userId.value) { console.warn('[Notifications] Cannot connect: no userId'); return }

        // Register (or re-register) notification topic before connecting
        unregisterNotifTopic?.()
        unregisterNotifTopic = registerTopic(`/topic/notifications/${userId.value}`, (body) => {
            try { handleIncoming(JSON.parse(body)) }
            catch (e) { console.error('[Notifications] Failed to parse notification:', e) }
        })

        wsConnect()
    }

    function disconnect() {
        unregisterNotifTopic?.()
        unregisterNotifTopic = null
        wsDisconnect()
    }

    async function fetchNotifications(page = 0, size = 50, unreadOnly = false) {
        try {
            const params: any = { page, size }
            if (unreadOnly) params.unread = true
            const { data } = await api.get('/api/notifications', { params })
            if (Array.isArray(data.content)) {
                const fresh = data.content.filter(
                    (n: Notification) => !notifications.value.some(e => e.id === n.id)
                )
                notifications.value.push(...fresh)
                notifications.value.sort((a, b) =>
                    new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
                )
            }
        } catch (e) { console.error('[Notifications] Failed to fetch:', e) }
    }

    async function fetchUnreadCount() {
        try {
            const { data } = await api.get('/api/notifications/unread-count')
            unreadCount.value = data.count || 0
        } catch (e) { console.error('[Notifications] Failed to fetch unread count:', e) }
    }

    async function markAsRead(id: number) {
        await api.patch(`/api/notifications/${id}/read`)
        const n = notifications.value.find(n => n.id === id)
        if (n && !n.isRead) { n.isRead = true; n.readAt = new Date().toISOString(); unreadCount.value = Math.max(0, unreadCount.value - 1) }
    }

    async function markAllAsRead() {
        const { data } = await api.patch('/api/notifications/mark-all-read')
        const now = new Date().toISOString()
        notifications.value.forEach(n => { if (!n.isRead) { n.isRead = true; n.readAt = now } })
        unreadCount.value = 0
        return data.updatedCount || 0
    }

    async function deleteNotification(id: number) {
        await api.delete(`/api/notifications/${id}`)
        const idx = notifications.value.findIndex(n => n.id === id)
        if (idx !== -1) {
            if (!notifications.value[idx].isRead) unreadCount.value = Math.max(0, unreadCount.value - 1)
            notifications.value.splice(idx, 1)
        }
    }

    async function deleteAllNotifications() {
        const { data } = await api.delete('/api/notifications')
        notifications.value = []
        unreadCount.value = 0
        return data.deletedCount || 0
    }

    async function requestNotificationPermission(): Promise<NotificationPermission | null> {
        if (typeof Notification === 'undefined') return null
        if (Notification.permission !== 'default') return Notification.permission
        try { return await Notification.requestPermission() }
        catch { return null }
    }

    const notificationPermission = computed(() => {
        if (typeof Notification === 'undefined') return 'unsupported' as const
        return Notification.permission
    })

    return {
        notifications: computed(() => notifications.value),
        unreadCount: computed(() => unreadCount.value),
        connected,
        notificationPermission,
        connect,
        disconnect,
        initListeners,
        destroyListeners,
        fetchNotifications,
        fetchUnreadCount,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        deleteAllNotifications,
        requestNotificationPermission,
    }
}
