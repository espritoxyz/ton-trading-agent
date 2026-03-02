<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, type Component } from 'vue'
import { Bell, X, Trash2, CheckCheck, WifiOff, Wifi, Wallet, CircleCheck, ArrowLeftRight, ClipboardCheck, TrendingUpDown } from 'lucide-vue-next'
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
    deleteAllNotifications,
    requestNotificationPermission,
    connect,
    disconnect,
    initListeners,
    destroyListeners
} = useNotifications()

const showDropdown = ref(false)
const buttonRef = ref<HTMLElement>()
const dropdownRef = ref<HTMLElement>()
const showPermissionPrompt = ref(false)
const dropdownStyle = ref<{ top: string; right: string }>({ top: '0px', right: '0px' })
const isMobile = ref(window.innerWidth < 768)

function updateIsMobile() {
    isMobile.value = window.innerWidth < 768
}

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

    initListeners()
    document.addEventListener('click', handleClickOutside)
    window.addEventListener('resize', updateIsMobile)
})

onUnmounted(() => {
    destroyListeners()
    document.removeEventListener('click', handleClickOutside)
    window.removeEventListener('resize', updateIsMobile)
})

function handleClickOutside(event: MouseEvent) {
    if (
        showDropdown.value &&
        !isMobile.value &&
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

    if (showDropdown.value && !isMobile.value && buttonRef.value) {
        // Calculate position based on button location (desktop only)
        const rect = buttonRef.value.getBoundingClientRect()
        const viewportWidth = window.innerWidth

        // Position dropdown below the button, aligned to the right
        dropdownStyle.value = {
            top: `${rect.bottom + 8}px`,
            right: `${viewportWidth - rect.right}px`
        }
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

async function handleDeleteAll() {
    try {
        await deleteAllNotifications()
    } catch (e) {
        console.error('Failed to delete all notifications:', e)
    }
}

async function handleRequestPermission() {
    const permission = await requestNotificationPermission()
    if (permission === 'granted' || permission === 'denied') {
        showPermissionPrompt.value = false
    }
}

function getNotificationIcon(type: NotificationType): Component {
    switch (type) {
        case 'BALANCE_CHANGE':
            return Wallet
        case 'TRANSACTION_COMPLETE':
            return CircleCheck
        case 'SWAP_EXECUTED':
            return ArrowLeftRight
        case 'ORDER_FILLED':
            return ClipboardCheck
        case 'TRACKER_TRIGGERED':
            return TrendingUpDown
        default:
            return Bell
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
            class="relative p-2.5 rounded-xl hover:bg-gradient-to-br hover:from-gray-100 hover:to-gray-50 dark:hover:from-gray-800 dark:hover:to-gray-700 transition-all duration-200 hover:scale-105 active:scale-95 group"
            :class="{ 'bg-gradient-to-br from-gray-100 to-gray-50 dark:from-gray-800 dark:to-gray-700 shadow-sm': showDropdown }"
        >
            <Bell
                :size="20"
                class="text-gray-700 dark:text-gray-300 transition-transform duration-200"
                :class="{ 'group-hover:rotate-12': !showDropdown, 'rotate-12': showDropdown }"
            />

            <!-- Unread Count Badge -->
            <span
                v-if="hasUnread"
                class="absolute -top-1 -right-1 bg-gradient-to-br from-red-500 to-red-600 text-white text-xs font-bold rounded-full min-w-[18px] h-[18px] flex items-center justify-center px-1 leading-none shadow-lg shadow-red-500/50 ring-2 ring-white dark:ring-gray-900"
            >
                {{ unreadCount > 99 ? '99+' : unreadCount }}
            </span>
        </button>

        <Teleport to="body">
            <!-- ── MOBILE: Bottom Sheet ── -->
            <template v-if="isMobile">
                <!-- Backdrop -->
                <Transition name="backdrop">
                    <div
                        v-if="showDropdown"
                        class="fixed inset-0 bg-black/40 backdrop-blur-sm z-[9998]"
                        @click="showDropdown = false"
                    />
                </Transition>

                <!-- Sheet -->
                <Transition name="sheet">
                    <div
                        v-if="showDropdown"
                        ref="dropdownRef"
                        class="fixed bottom-0 left-0 right-0 z-[9999] bg-white dark:bg-gray-900 rounded-t-2xl shadow-2xl flex flex-col"
                        style="max-height: 85dvh;"
                    >
                        <!-- Drag handle -->
                        <div class="flex justify-center pt-3 pb-1 shrink-0">
                            <div class="w-10 h-1 rounded-full bg-gray-300 dark:bg-gray-600" />
                        </div>

                        <!-- Header -->
                        <div class="px-5 pt-2 pb-4 border-b border-gray-200/60 dark:border-gray-700/60 shrink-0">
                            <div class="flex items-center justify-between mb-3">
                                <h3 class="text-lg font-bold bg-gradient-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
                                    Notifications
                                </h3>
                                <div class="flex items-center gap-2">
                                    <!-- Mark All as Read -->
                                    <button
                                        v-if="hasUnread"
                                        @click="handleMarkAllAsRead"
                                        class="p-1.5 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-all duration-150 group"
                                        title="Mark all as read"
                                    >
                                        <CheckCheck :size="14" class="text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors" :strokeWidth="2.5" />
                                    </button>

                                    <!-- Clear All -->
                                    <button
                                        v-if="hasNotifications"
                                        @click="handleDeleteAll"
                                        class="p-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30 transition-all duration-150 group"
                                        title="Clear all notifications"
                                    >
                                        <Trash2 :size="14" class="text-gray-400 group-hover:text-red-600 dark:group-hover:text-red-400 transition-colors" :strokeWidth="2.5" />
                                    </button>

                                    <!-- Connection Status -->
                                    <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-gray-100 dark:bg-gray-800 border border-gray-200/50 dark:border-gray-700/50">
                                        <component :is="connected ? Wifi : WifiOff" :size="12" :class="connected ? 'text-green-500' : 'text-gray-400'" />
                                        <span class="text-xs font-medium text-gray-600 dark:text-gray-300">{{ connected ? 'Live' : 'Offline' }}</span>
                                    </div>

                                    <!-- Close Button -->
                                    <button
                                        @click="showDropdown = false"
                                        class="p-1.5 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-all duration-150"
                                    >
                                        <X :size="16" class="text-gray-500 dark:text-gray-400" :strokeWidth="2.5" />
                                    </button>
                                </div>
                            </div>

                            <!-- Permission Prompt -->
                            <div
                                v-if="showPermissionPrompt && notificationPermission === 'default'"
                                class="mt-1 p-3.5 bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl text-sm border border-blue-200/50 dark:border-blue-700/50"
                            >
                                <p class="text-blue-900 dark:text-blue-100 mb-2.5 font-medium">
                                    Enable desktop notifications to stay updated even when the app is in the background.
                                </p>
                                <button
                                    @click="handleRequestPermission"
                                    class="w-full px-3 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-lg text-xs font-semibold shadow-lg shadow-blue-500/30 transition-all duration-200"
                                >
                                    Enable Notifications
                                </button>
                            </div>
                        </div>

                        <!-- Notifications List -->
                        <div class="overflow-y-auto flex-1 notification-list">
                            <!-- Empty State -->
                            <div v-if="!hasNotifications" class="p-12 text-center text-gray-500 dark:text-gray-400">
                                <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 mb-4">
                                    <Bell :size="36" class="opacity-40" />
                                </div>
                                <p class="text-sm font-semibold text-gray-700 dark:text-gray-300">No notifications yet</p>
                                <p class="text-xs mt-1.5 text-gray-500 dark:text-gray-400">You'll see updates about your wallet activity here</p>
                            </div>

                            <!-- Notification Items -->
                            <div v-else class="p-2">
                                <div
                                    v-for="(notification, index) in displayNotifications"
                                    :key="notification.id"
                                    @click="!notification.isRead && markAsRead(notification.id)"
                                    class="mb-1.5 p-3 rounded-xl transition-all duration-200 notification-item group relative"
                                    :class="[
                                        !notification.isRead
                                            ? 'bg-gradient-to-br from-blue-50/80 to-indigo-50/60 dark:from-blue-900/20 dark:to-indigo-900/15 border border-blue-200/50 dark:border-blue-700/40 shadow-sm'
                                            : 'bg-white/40 dark:bg-gray-800/30 border border-gray-200/40 dark:border-gray-700/40',
                                    ]"
                                    :style="{ animationDelay: `${index * 30}ms` }"
                                >
                                    <!-- Delete Button -->
                                    <button
                                        @click.stop="handleDelete(notification.id, $event)"
                                        class="absolute top-2 right-2 p-1.5 rounded-md hover:bg-red-100 dark:hover:bg-red-900/30 transition-all duration-150"
                                        title="Delete notification"
                                    >
                                        <X :size="13" class="text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors" :strokeWidth="2.5" />
                                    </button>

                                    <div class="flex items-start gap-3">
                                        <!-- Icon -->
                                        <div class="flex-shrink-0 mt-0.5">
                                            <div class="p-2 rounded-lg bg-gradient-to-br"
                                                :class="!notification.isRead
                                                    ? 'from-blue-100 to-indigo-100 dark:from-blue-900/40 dark:to-indigo-900/40'
                                                    : 'from-gray-100 to-gray-200 dark:from-gray-700 dark:to-gray-600'"
                                            >
                                                <component
                                                    :is="getNotificationIcon(notification.type)"
                                                    :size="16"
                                                    :class="!notification.isRead ? 'text-blue-600 dark:text-blue-400' : 'text-gray-600 dark:text-gray-400'"
                                                    :strokeWidth="2.5"
                                                />
                                            </div>
                                        </div>

                                        <!-- Content -->
                                        <div class="flex-1 min-w-0 pr-6">
                                            <div class="flex items-start justify-between gap-2 mb-0.5">
                                                <h4 class="text-sm font-bold text-gray-900 dark:text-white leading-tight">
                                                    {{ notification.title }}
                                                </h4>
                                                <span class="text-[11px] text-gray-500 dark:text-gray-400 flex-shrink-0 font-medium mt-0.5">
                                                    {{ formatTimestamp(notification.createdAt) }}
                                                </span>
                                            </div>
                                            <p class="text-xs text-gray-600 dark:text-gray-300 line-clamp-2 leading-snug">
                                                {{ notification.message }}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            </div>

                            <!-- Safe area padding for home indicator -->
                            <div class="h-safe-bottom" style="height: env(safe-area-inset-bottom, 16px);" />
                        </div>
                    </div>
                </Transition>
            </template>

            <!-- ── DESKTOP: Dropdown ── -->
            <template v-else>
                <Transition name="dropdown">
                    <div
                        v-if="showDropdown"
                        ref="dropdownRef"
                        class="fixed w-96 bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl rounded-2xl shadow-2xl border border-gray-200/50 dark:border-gray-700/50 z-[9999] max-h-[600px] flex flex-col overflow-hidden"
                        :style="dropdownStyle"
                    >
                    <!-- Header -->
                    <div class="p-5 border-b border-gray-200/50 dark:border-gray-700/50 bg-gradient-to-b from-gray-50/50 to-transparent dark:from-gray-800/30">
                        <div class="flex items-center justify-between mb-3">
                            <h3 class="text-lg font-bold bg-gradient-to-r from-gray-900 to-gray-600 dark:from-white dark:to-gray-300 bg-clip-text text-transparent">
                                Notifications
                            </h3>
                            <div class="flex items-center gap-2">
                                <!-- Mark All as Read Button -->
                                <button
                                    v-if="hasUnread"
                                    @click="handleMarkAllAsRead"
                                    class="p-1.5 rounded-lg hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-all duration-150 group"
                                    title="Mark all as read"
                                >
                                    <CheckCheck
                                        :size="14"
                                        class="text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors"
                                        :strokeWidth="2.5"
                                    />
                                </button>

                                <!-- Clear All Button -->
                                <button
                                    v-if="hasNotifications"
                                    @click="handleDeleteAll"
                                    class="p-1.5 rounded-lg hover:bg-red-100 dark:hover:bg-red-900/30 transition-all duration-150 group"
                                    title="Clear all notifications"
                                >
                                    <Trash2
                                        :size="14"
                                        class="text-gray-400 group-hover:text-red-600 dark:group-hover:text-red-400 transition-colors"
                                        :strokeWidth="2.5"
                                    />
                                </button>

                                <!-- Connection Status -->
                                <div class="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/60 dark:bg-gray-800/60 backdrop-blur-sm border border-gray-200/50 dark:border-gray-700/50">
                                    <component
                                        :is="connected ? Wifi : WifiOff"
                                        :size="12"
                                        :class="connected ? 'text-green-500' : 'text-gray-400'"
                                    />
                                    <span class="text-xs font-medium text-gray-600 dark:text-gray-300">
                                        {{ connected ? 'Live' : 'Offline' }}
                                    </span>
                                </div>
                            </div>
                        </div>

                        <!-- Permission Prompt -->
                        <div
                            v-if="showPermissionPrompt && notificationPermission === 'default'"
                            class="mt-3 p-3.5 bg-gradient-to-br from-blue-50 to-indigo-50 dark:from-blue-900/20 dark:to-indigo-900/20 rounded-xl text-sm border border-blue-200/50 dark:border-blue-700/50 backdrop-blur-sm"
                        >
                            <p class="text-blue-900 dark:text-blue-100 mb-2.5 font-medium">
                                Enable desktop notifications to stay updated even when the app is in the background.
                            </p>
                            <button
                                @click="handleRequestPermission"
                                class="w-full px-3 py-2 bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700 text-white rounded-lg text-xs font-semibold shadow-lg shadow-blue-500/30 transition-all duration-200 hover:shadow-xl hover:shadow-blue-500/40 hover:scale-[1.02]"
                            >
                                Enable Notifications
                            </button>
                        </div>
                    </div>

                    <!-- Notifications List -->
                    <div class="overflow-y-auto flex-1 notification-list">
                        <!-- Empty State -->
                        <div
                            v-if="!hasNotifications"
                            class="p-12 text-center text-gray-500 dark:text-gray-400"
                        >
                            <div class="inline-flex items-center justify-center w-20 h-20 rounded-full bg-gradient-to-br from-gray-100 to-gray-200 dark:from-gray-800 dark:to-gray-700 mb-4">
                                <Bell :size="36" class="opacity-40" />
                            </div>
                            <p class="text-sm font-semibold text-gray-700 dark:text-gray-300">No notifications yet</p>
                            <p class="text-xs mt-1.5 text-gray-500 dark:text-gray-400">You'll see updates about your wallet activity here</p>
                        </div>

                        <!-- Notification Items -->
                        <div v-else class="p-2">
                            <div
                                v-for="(notification, index) in displayNotifications"
                                :key="notification.id"
                                @mouseenter="!notification.isRead && markAsRead(notification.id)"
                                class="mb-1.5 p-2.5 rounded-xl transition-all duration-200 notification-item group relative"
                                :class="[
                                    !notification.isRead
                                        ? 'bg-gradient-to-br from-blue-50/80 to-indigo-50/60 dark:from-blue-900/20 dark:to-indigo-900/15 border border-blue-200/50 dark:border-blue-700/40 shadow-sm hover:shadow-md'
                                        : 'bg-white/40 dark:bg-gray-800/30 border border-gray-200/40 dark:border-gray-700/40 hover:bg-white/60 dark:hover:bg-gray-800/50 hover:shadow-sm',
                                    'hover:scale-[1.01] hover:border-gray-300/60 dark:hover:border-gray-600/60'
                                ]"
                                :style="{ animationDelay: `${index * 30}ms` }"
                            >
                                <!-- Delete Button (Top Right) -->
                                <button
                                    @click="handleDelete(notification.id, $event)"
                                    class="absolute top-1.5 right-1.5 p-1 rounded-md opacity-0 group-hover:opacity-100 hover:bg-red-100 dark:hover:bg-red-900/30 transition-all duration-150 hover:scale-110 z-10"
                                    title="Delete notification"
                                >
                                    <X
                                        :size="12"
                                        class="text-gray-400 hover:text-red-600 dark:hover:text-red-400 transition-colors"
                                        :strokeWidth="2.5"
                                    />
                                </button>

                                <div class="flex items-start gap-2.5">
                                    <!-- Icon -->
                                    <div class="flex-shrink-0 mt-0.5">
                                        <div class="p-1.5 rounded-lg bg-gradient-to-br transition-all duration-200 group-hover:scale-110"
                                            :class="!notification.isRead
                                                ? 'from-blue-100 to-indigo-100 dark:from-blue-900/40 dark:to-indigo-900/40'
                                                : 'from-gray-100 to-gray-200 dark:from-gray-700 dark:to-gray-600'"
                                        >
                                            <component
                                                :is="getNotificationIcon(notification.type)"
                                                :size="16"
                                                :class="!notification.isRead
                                                    ? 'text-blue-600 dark:text-blue-400'
                                                    : 'text-gray-600 dark:text-gray-400'"
                                                :strokeWidth="2.5"
                                            />
                                        </div>
                                    </div>

                                    <!-- Content -->
                                    <div class="flex-1 min-w-0 pr-4">
                                        <div class="flex items-start justify-between gap-2 mb-0.5">
                                            <h4 class="text-xs font-bold text-gray-900 dark:text-white leading-tight">
                                                {{ notification.title }}
                                            </h4>
                                            <span class="text-[10px] text-gray-500 dark:text-gray-400 flex-shrink-0 font-medium mt-0.5">
                                                {{ formatTimestamp(notification.createdAt) }}
                                            </span>
                                        </div>
                                        <p class="text-[11px] text-gray-600 dark:text-gray-300 line-clamp-2 leading-snug">
                                            {{ notification.message }}
                                        </p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    </div>
                </Transition>
            </template>
        </Teleport>
    </div>
</template>

<style scoped>
/* ── Desktop Dropdown Animations ── */
.dropdown-enter-active {
    transition: opacity 0.25s cubic-bezier(0.4, 0, 0.2, 1),
                transform 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-leave-active {
    transition: opacity 0.2s cubic-bezier(0.4, 0, 0.2, 1),
                transform 0.2s cubic-bezier(0.4, 0, 0.2, 1);
}

.dropdown-enter-from {
    opacity: 0;
    transform: translateY(-12px) scale(0.95);
}

.dropdown-leave-to {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
}

/* ── Mobile Bottom Sheet Animations ── */
.sheet-enter-active {
    transition: transform 0.35s cubic-bezier(0.32, 0.72, 0, 1);
}

.sheet-leave-active {
    transition: transform 0.28s cubic-bezier(0.32, 0.72, 0, 1);
}

.sheet-enter-from,
.sheet-leave-to {
    transform: translateY(100%);
}

/* ── Backdrop Animations ── */
.backdrop-enter-active {
    transition: opacity 0.3s ease;
}

.backdrop-leave-active {
    transition: opacity 0.25s ease;
}

.backdrop-enter-from,
.backdrop-leave-to {
    opacity: 0;
}

/* ── Custom Scrollbar ── */
.notification-list::-webkit-scrollbar {
    width: 6px;
}

.notification-list::-webkit-scrollbar-track {
    background: transparent;
}

.notification-list::-webkit-scrollbar-thumb {
    background: rgba(156, 163, 175, 0.3);
    border-radius: 10px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
    background: rgba(156, 163, 175, 0.5);
}

.dark .notification-list::-webkit-scrollbar-thumb {
    background: rgba(107, 114, 128, 0.3);
}

.dark .notification-list::-webkit-scrollbar-thumb:hover {
    background: rgba(107, 114, 128, 0.5);
}

/* ── Notification Item Animations ── */
.notification-item {
    animation: slideIn 0.3s cubic-bezier(0.4, 0, 0.2, 1) backwards;
}

@keyframes slideIn {
    from {
        opacity: 0;
        transform: translateX(-10px);
    }
    to {
        opacity: 1;
        transform: translateX(0);
    }
}

/* ── Line Clamp ── */
.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
