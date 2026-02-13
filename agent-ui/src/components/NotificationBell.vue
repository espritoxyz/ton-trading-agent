<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { Bell, X, Check, Trash2, CheckCheck, WifiOff, Wifi, Loader } from 'lucide-vue-next'
import { useNotifications } from '../composables/useNotifications'
import { accessToken, userId } from '../composables/useAuth'
import type { Notification, NotificationType } from '../types'

const {
    notifications,
    unreadCount,
    connected,
    notificationPermission,
    fetchNotifications,
    fetchUnreadCount,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    requestNotificationPermission,
    connect,
    disconnect
} = useNotifications()

const showDropdown = ref(false)
const buttonRef = ref<HTMLElement>()
const dropdownRef = ref<HTMLElement>()
const showPermissionPrompt = ref(false)
const dropdownStyle = ref<{ top: string; right: string }>({ top: '0px', right: '0px' })

// Watch for authentication changes and connect/disconnect accordingly
watch([accessToken, userId], ([token, uid]) => {
    if (token && uid) {
        // User is authenticated, connect
        connect()
        fetchNotifications()
        fetchUnreadCount()
    } else {
        // User logged out, disconnect
        disconnect()
    }
}, { immediate: true })

// Initialize on mount
onMounted(async () => {
    // Check if we should show permission prompt
    if (notificationPermission.value === 'default') {
        showPermissionPrompt.value = true
    }

    document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
    disconnect()
    document.removeEventListener('click', handleClickOutside)
})

function handleClickOutside(event: MouseEvent) {
    if (
        showDropdown.value &&
        dropdownRef.value &&
        !dropdownRef.value.contains(event.target as Node) &&
        buttonRef.value &&
        !buttonRef.value.contains(event.target as Node)
    ) {
        showDropdown.value = false
    }
}

function toggleDropdown() {
    showDropdown.value = !showDropdown.value

    if (showDropdown.value && buttonRef.value) {
        // Calculate position based on button location
        const rect = buttonRef.value.getBoundingClientRect()
        const viewportWidth = window.innerWidth

        // Position dropdown below the button, aligned to the right
        dropdownStyle.value = {
            top: `${rect.bottom + 8}px`,
            right: `${viewportWidth - rect.right}px`
        }
    }
}

async function handleMarkAsRead(id: number, event: Event) {
    event.stopPropagation()
    try {
        await markAsRead(id)
    } catch (e) {
        console.error('Failed to mark notification as read:', e)
    }
}

async function handleDelete(id: number, event: Event) {
    event.stopPropagation()
    try {
        await deleteNotification(id)
    } catch (e) {
        console.error('Failed to delete notification:', e)
    }
}

async function handleMarkAllAsRead() {
    try {
        await markAllAsRead()
    } catch (e) {
        console.error('Failed to mark all as read:', e)
    }
}

async function handleRequestPermission() {
    const permission = await requestNotificationPermission()
    if (permission === 'granted' || permission === 'denied') {
        showPermissionPrompt.value = false
    }
}

function getNotificationIcon(type: NotificationType) {
    switch (type) {
        case 'BALANCE_CHANGE':
            return '💰'
        case 'TRANSACTION_COMPLETE':
            return '✅'
        case 'SWAP_EXECUTED':
            return '🔄'
        case 'ORDER_FILLED':
            return '📈'
        default:
            return '🔔'
    }
}

function formatTimestamp(timestamp: string) {
    const date = new Date(timestamp)
    const now = new Date()
    const diffMs = now.getTime() - date.getTime()
    const diffMins = Math.floor(diffMs / 60000)
    const diffHours = Math.floor(diffMs / 3600000)
    const diffDays = Math.floor(diffMs / 86400000)

    if (diffMins < 1) return 'Just now'
    if (diffMins < 60) return `${diffMins}m ago`
    if (diffHours < 24) return `${diffHours}h ago`
    if (diffDays < 7) return `${diffDays}d ago`
    return date.toLocaleDateString()
}

const hasUnread = computed(() => unreadCount.value > 0)
const displayNotifications = computed(() => notifications.value.slice(0, 20))
const hasNotifications = computed(() => notifications.value.length > 0)
const isAuthenticated = computed(() => !!accessToken.value && !!userId.value)
</script>

<template>
    <div v-if="isAuthenticated" class="relative">
        <!-- Notification Bell Button -->
        <button
            ref="buttonRef"
            @click="toggleDropdown"
            class="relative p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
            :class="{ 'bg-gray-100 dark:bg-gray-800': showDropdown }"
        >
            <Bell :size="20" class="text-gray-700 dark:text-gray-300" />

            <!-- Unread Count Badge -->
            <span
                v-if="hasUnread"
                class="absolute -top-1 -right-1 bg-red-500 text-white text-xs font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1"
            >
                {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
        </button>

        <!-- Dropdown Panel (Teleported to body) -->
        <Teleport to="body">
            <Transition name="dropdown">
                <div
                    v-if="showDropdown"
                    ref="dropdownRef"
                    class="fixed w-96 bg-white dark:bg-gray-900 rounded-lg shadow-xl border border-gray-200 dark:border-gray-700 z-[9999] max-h-[600px] flex flex-col"
                    :style="dropdownStyle"
                >
                <!-- Header -->
                <div class="p-4 border-b border-gray-200 dark:border-gray-700">
                    <div class="flex items-center justify-between mb-2">
                        <h3 class="text-lg font-semibold text-gray-900 dark:text-white">
                            Notifications
                        </h3>
                        <div class="flex items-center gap-2">
                            <!-- Connection Status -->
                            <div class="flex items-center gap-1 text-xs">
                                <component
                                    :is="connected ? Wifi : WifiOff"
                                    :size="14"
                                    :class="connected ? 'text-green-500' : 'text-gray-400'"
                                />
                                <span class="text-gray-500 dark:text-gray-400">
                                    {{ connected ? 'Connected' : 'Disconnected' }}
                                </span>
                            </div>
                        </div>
                    </div>

                    <!-- Permission Prompt -->
                    <div
                        v-if="showPermissionPrompt && notificationPermission === 'default'"
                        class="mt-2 p-2 bg-blue-50 dark:bg-blue-900/20 rounded-md text-sm"
                    >
                        <p class="text-blue-900 dark:text-blue-200 mb-2">
                            Enable desktop notifications to stay updated even when the app is in the background.
                        </p>
                        <button
                            @click="handleRequestPermission"
                            class="w-full px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-md text-xs font-medium"
                        >
                            Enable Notifications
                        </button>
                    </div>

                    <!-- Mark All as Read -->
                    <button
                        v-if="hasUnread"
                        @click="handleMarkAllAsRead"
                        class="mt-2 text-sm text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
                    >
                        <CheckCheck :size="14" />
                        Mark all as read
                    </button>
                </div>

                <!-- Notifications List -->
                <div class="overflow-y-auto flex-1">
                    <!-- Empty State -->
                    <div
                        v-if="!hasNotifications"
                        class="p-8 text-center text-gray-500 dark:text-gray-400"
                    >
                        <Bell :size="48" class="mx-auto mb-2 opacity-20" />
                        <p class="text-sm">No notifications yet</p>
                        <p class="text-xs mt-1">You'll see updates about your wallet activity here</p>
                    </div>

                    <!-- Notification Items -->
                    <div v-else>
                        <div
                            v-for="notification in displayNotifications"
                            :key="notification.id"
                            class="p-4 border-b border-gray-100 dark:border-gray-800 hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"
                            :class="{ 'bg-blue-50 dark:bg-blue-900/10': !notification.isRead }"
                        >
                            <div class="flex items-start gap-3">
                                <!-- Icon -->
                                <div class="flex-shrink-0 text-2xl">
                                    {{ getNotificationIcon(notification.type) }}
                                </div>

                                <!-- Content -->
                                <div class="flex-1 min-w-0">
                                    <div class="flex items-start justify-between gap-2">
                                        <h4 class="text-sm font-semibold text-gray-900 dark:text-white">
                                            {{ notification.title }}
                                        </h4>
                                        <span class="text-xs text-gray-500 dark:text-gray-400 flex-shrink-0">
                                            {{ formatTimestamp(notification.createdAt) }}
                                        </span>
                                    </div>
                                    <p class="text-sm text-gray-600 dark:text-gray-300 mt-1 line-clamp-2">
                                        {{ notification.message }}
                                    </p>

                                    <!-- Actions -->
                                    <div class="flex items-center gap-2 mt-2">
                                        <button
                                            v-if="!notification.isRead"
                                            @click="handleMarkAsRead(notification.id, $event)"
                                            class="text-xs text-blue-600 dark:text-blue-400 hover:underline flex items-center gap-1"
                                        >
                                            <Check :size="12" />
                                            Mark as read
                                        </button>
                                        <button
                                            @click="handleDelete(notification.id, $event)"
                                            class="text-xs text-red-600 dark:text-red-400 hover:underline flex items-center gap-1"
                                        >
                                            <Trash2 :size="12" />
                                            Delete
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                </div>
            </Transition>
        </Teleport>
    </div>
</template>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
    transition: opacity 0.2s, transform 0.2s;
}

.dropdown-enter-from,
.dropdown-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}

.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
