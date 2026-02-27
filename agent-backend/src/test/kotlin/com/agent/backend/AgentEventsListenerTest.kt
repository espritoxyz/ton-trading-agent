package com.agent.backend

import com.agent.backend.db.entity.NotificationType
import com.agent.backend.llm.ChatJobService
import com.agent.backend.rabbitmq.listeners.AgentEventsListener
import com.agent.backend.service.ExternalToolResultService
import com.agent.backend.service.NotificationEventPublisher
import com.agent.backend.service.NotificationService
import com.agent.backend.service.StonfiAssetsCacheService
import com.agent.backend.service.WalletService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import java.util.UUID

// Kotlin/Mockito null-safety helpers
private fun <T> any(type: Class<T>): T = ArgumentMatchers.any(type)
private fun anyLong(): Long = ArgumentMatchers.anyLong()
private fun anyString(): String = ArgumentMatchers.anyString() ?: ""
private fun <K, V> anyMap(): Map<K, V> = ArgumentMatchers.anyMap()
// Unconstrained T prevents Kotlin from adding intrinsic null checks on ArgumentCaptor.capture()
private fun <T> captureArg(captor: ArgumentCaptor<T>): T = captor.capture()

class AgentEventsListenerTest {

    private lateinit var jobService: ChatJobService
    private lateinit var externalToolResultService: ExternalToolResultService
    private lateinit var walletService: WalletService
    private lateinit var notificationEventPublisher: NotificationEventPublisher
    private lateinit var notificationService: NotificationService
    private lateinit var assetsCache: StonfiAssetsCacheService
    private lateinit var listener: AgentEventsListener

    @BeforeEach
    fun setup() {
        jobService = mock(ChatJobService::class.java)
        externalToolResultService = mock(ExternalToolResultService::class.java)
        walletService = mock(WalletService::class.java)
        notificationEventPublisher = mock(NotificationEventPublisher::class.java)
        notificationService = mock(NotificationService::class.java)
        assetsCache = mock(StonfiAssetsCacheService::class.java)

        listener = AgentEventsListener(
            jobService,
            externalToolResultService,
            walletService,
            notificationEventPublisher,
            notificationService,
            assetsCache
        )

        `when`(
            notificationService.generateNotificationText(
                any(NotificationType::class.java),
                anyMap()
            )
        ).thenReturn(Pair("Swap Executed", "Swap notification"))
    }

    // ── publishSwapTonToTokenNotification ─────────────────────────────────────

    @Test
    fun `publishSwapTonToTokenNotification uses symbol from asset cache`() {
        val jettonMaster = "EQAsset123"
        `when`(assetsCache.getAssetByContractAddress(jettonMaster)).thenReturn(
            StonfiAssetsCacheService.StonfiAsset(
                contractAddress = jettonMaster,
                symbol = "USDT",
                dexUsdPriceString = "5.0",
                decimals = 6
            )
        )

        listener.onEvent(swapTonToTokenResult(jettonMaster = jettonMaster, swapTonAmount = 1.5, minimalTokenAmount = 100.0))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, Any>>
        verify(notificationEventPublisher).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), captureArg(captor)
        )
        val metadata = captor.value
        assertEquals("TON", metadata["fromAsset"])
        assertEquals("USDT", metadata["toAsset"])
        assertEquals("1.5", metadata["fromAmount"])
        assertEquals("100.0", metadata["toAmount"])
    }

    @Test
    fun `publishSwapTonToTokenNotification falls back to contract address when asset not in cache`() {
        val jettonMaster = "EQUnknown456"
        `when`(assetsCache.getAssetByContractAddress(jettonMaster)).thenReturn(null)

        listener.onEvent(swapTonToTokenResult(jettonMaster = jettonMaster, swapTonAmount = 2.0))

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, Any>>
        verify(notificationEventPublisher).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), captureArg(captor)
        )
        assertEquals(jettonMaster, captor.value["toAsset"])
    }

    @Test
    fun `publishSwapTonToTokenNotification is not called on failure`() {
        listener.onEvent(swapTonToTokenResult(success = false, error = "Insufficient balance"))

        verify(notificationEventPublisher, never()).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), anyMap()
        )
    }

    // ── publishSwapTokenToTonNotification ─────────────────────────────────────

    @Test
    fun `publishSwapTokenToTonNotification converts nano amount using asset decimals`() {
        val jettonMaster = "EQUsdt789"
        `when`(assetsCache.getAssetByContractAddress(jettonMaster)).thenReturn(
            StonfiAssetsCacheService.StonfiAsset(
                contractAddress = jettonMaster,
                symbol = "USDT",
                dexUsdPriceString = "1.0",
                decimals = 6
            )
        )

        listener.onEvent(
            swapTokenToTonResult(
                jettonMaster = jettonMaster,
                swapTokenAmountNano = 5_000_000L,  // 5.0 USDT at 6 decimals
                minimalTonAmount = 0.5
            )
        )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, Any>>
        verify(notificationEventPublisher).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), captureArg(captor)
        )
        val metadata = captor.value
        assertEquals("USDT", metadata["fromAsset"])
        assertEquals("TON", metadata["toAsset"])
        assertEquals("5", metadata["fromAmount"])
        assertEquals("0.5", metadata["toAmount"])
    }

    @Test
    fun `publishSwapTokenToTonNotification defaults to 9 decimals when asset decimals unknown`() {
        val jettonMaster = "EQJetton"
        `when`(assetsCache.getAssetByContractAddress(jettonMaster)).thenReturn(null)

        listener.onEvent(
            swapTokenToTonResult(
                jettonMaster = jettonMaster,
                swapTokenAmountNano = 1_500_000_000L,  // 1.5 tokens at 9 decimals
                minimalTonAmount = 1.0
            )
        )

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(Map::class.java) as ArgumentCaptor<Map<String, Any>>
        verify(notificationEventPublisher).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), captureArg(captor)
        )
        assertEquals("1.5", captor.value["fromAmount"])
    }

    @Test
    fun `publishSwapTokenToTonNotification is not called on failure`() {
        listener.onEvent(swapTokenToTonResult(success = false, error = "Swap failed"))

        verify(notificationEventPublisher, never()).publishNotificationEvent(
            anyLong(), anyString(), anyString(), anyString(), anyMap()
        )
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private fun swapTonToTokenResult(
        userId: Long = 1L,
        success: Boolean = true,
        txId: String? = "tx123",
        jettonMaster: String? = "EQDefault",
        swapTonAmount: Number? = 1.0,
        minimalTokenAmount: Number? = 50.0,
        error: String? = null,
    ): Map<String, Any?> = mapOf(
        "type" to "agent-llm.swap-ton-to-token.result",
        "data" to mapOf(
            "messageId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "success" to success,
            "txId" to txId,
            "requestedJettonMaster" to jettonMaster,
            "requestedSwapTonAmount" to swapTonAmount,
            "requestedMinimalTokenAmount" to minimalTokenAmount,
            "error" to error
        )
    )

    private fun swapTokenToTonResult(
        userId: Long = 1L,
        success: Boolean = true,
        txId: String? = "tx456",
        jettonMaster: String? = "EQDefault",
        swapTokenAmountNano: Number? = 1_000_000_000L,
        minimalTonAmount: Number? = 0.5,
        error: String? = null,
    ): Map<String, Any?> = mapOf(
        "type" to "agent-llm.swap-token-to-ton.result",
        "data" to mapOf(
            "messageId" to UUID.randomUUID().toString(),
            "userId" to userId,
            "success" to success,
            "txId" to txId,
            "requestedJettonMaster" to jettonMaster,
            "requestedSwapTokenAmount" to swapTokenAmountNano,
            "requestedMinimalTonAmount" to minimalTonAmount,
            "error" to error
        )
    )
}
