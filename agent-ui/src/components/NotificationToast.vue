<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { X } from 'lucide-vue-next'
import type { Notification, NotificationType } from '../types'
import { useNotifications } from '../composables/useNotifications'

interface ToastNotification extends Notification {
    progress: number
    timeoutId?: number
}

const { notifications, markAsRead } = useNotifications()
const toasts = ref<ToastNotification[]>([])
const TOAST_DURATION = 5000 // 5 seconds
const MAX_TOASTS = 3

// Watch for new notifications and create toasts
watch(notifications, (newNotifications, oldNotifications) => {
    // Find newly added notifications (not read)
    const newUnread = newNotifications.filter(
        n => !n.isRead && !oldNotifications.some(o => o.id === n.id)
    )

    newUnread.forEach(notification => {
        addToast(notification)
    })
})

function addToast(notification: Notification) {
    // Limit number of toasts
    if (toasts.value.length >= MAX_TOASTS) {
        const oldest = toasts.value[0]
        removeToast(oldest.id)
    }

    const toast: ToastNotification = {
        ...notification,
        progress: 100
    }

    toasts.value.push(toast)
    startToastTimer(toast)
}

function startToastTimer(toast: ToastNotification) {
    const startTime = Date.now()
    const interval = 50 // Update every 50ms

    const updateProgress = () => {
        const elapsed = Date.now() - startTime
        const remaining = Math.max(0, TOAST_DURATION - elapsed)
        toast.progress = (remaining / TOAST_DURATION) * 100

        if (remaining > 0) {
            toast.timeoutId = window.setTimeout(updateProgress, interval)
        } else {
            removeToast(toast.id)
        }
    }

    toast.timeoutId = window.setTimeout(updateProgress, interval)
}

function removeToast(id: number) {
    const index = toasts.value.findIndex(t => t.id === id)
    if (index !== -1) {
        const toast = toasts.value[index]
        if (toast.timeoutId) {
            clearTimeout(toast.timeoutId)
        }
        toasts.value.splice(index, 1)
    }
}

async function handleToastClick(toast: ToastNotification) {
    // Mark as read
    try {
        await markAsRead(toast.id)
    } catch (e) {
        console.error('Failed to mark notification as read:', e)
    }

    // Remove toast
    removeToast(toast.id)

    // Optional: Navigate to relevant page based on notification type
    // This can be implemented later based on specific requirements
}

function handleDismiss(toast: ToastNotification, event: Event) {
    event.stopPropagation()
    removeToast(toast.id)
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

function getToastColor(type: NotificationType) {
    switch (type) {
        case 'BALANCE_CHANGE':
            return 'bg-green-500'
        case 'TRANSACTION_COMPLETE':
            return 'bg-blue-500'
        case 'SWAP_EXECUTED':
            return 'bg-purple-500'
        case 'ORDER_FILLED':
            return 'bg-orange-500'
        default:
            return 'bg-gray-500'
    }
}
</script>

<template>
    <div class="fixed bottom-4 right-4 z-[100] space-y-2 pointer-events-none">
        <TransitionGroup name="toast">
            <div
                v-for="toast in toasts"
                :key="toast.id"
                @click="handleToastClick(toast)"
                class="bg-white dark:bg-gray-900 rounded-lg shadow-2xl border border-gray-200 dark:border-gray-700 w-80 overflow-hidden cursor-pointer hover:shadow-xl transition-all pointer-events-auto"
            >
                <!-- Content -->
                <div class="p-4">
                    <div class="flex items-start gap-3">
                        <!-- Icon -->
                        <div class="flex-shrink-0 text-2xl">
                            {{ getNotificationIcon(toast.type) }}
                        </div>

                        <!-- Text Content -->
                        <div class="flex-1 min-w-0">
                            <h4 class="text-sm font-semibold text-gray-900 dark:text-white mb-1">
                                {{ toast.title }}
                            </h4>
                            <p class="text-sm text-gray-600 dark:text-gray-300 line-clamp-2">
                                {{ toast.message }}
                            </p>
                        </div>

                        <!-- Dismiss Button -->
                        <button
                            @click="handleDismiss(toast, $event)"
                            class="flex-shrink-0 p-1 rounded-md hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                        >
                            <X :size="16" class="text-gray-500 dark:text-gray-400" />
                        </button>
                    </div>
                </div>

                <!-- Progress Bar -->
                <div class="h-1 bg-gray-200 dark:bg-gray-800">
                    <div
                        :class="getToastColor(toast.type)"
                        class="h-full transition-all duration-100 ease-linear"
                        :style="{ width: toast.progress + '%' }"
                    />
                </div>
            </div>
        </TransitionGroup>
    </div>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
    transition: all 0.3s ease;
}

.toast-enter-from {
    opacity: 0;
    transform: translateX(100%);
}

.toast-leave-to {
    opacity: 0;
    transform: translateX(100%) scale(0.8);
}

.toast-move {
    transition: transform 0.3s ease;
}

.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
</style>
