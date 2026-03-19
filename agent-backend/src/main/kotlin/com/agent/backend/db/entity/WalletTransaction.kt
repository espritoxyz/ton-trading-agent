package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "wallet_transaction")
class WalletTransaction(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "wallet_address", nullable = false)
    var walletAddress: String,

    @Column(name = "transaction_hash", nullable = false)
    var transactionHash: String,

    @Column(name = "transaction_lt", nullable = false)
    var transactionLt: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var direction: TransactionDirection,

    @Column(name = "amount_nano", nullable = false)
    var amountNano: Long,

    @Column(name = "asset_type", nullable = false)
    var assetType: String,

    @Column(name = "jetton_master_address")
    var jettonMasterAddress: String? = null,

    @Column(name = "jetton_symbol")
    var jettonSymbol: String? = null,

    @Column(name = "jetton_decimals")
    var jettonDecimals: Int? = null,

    @Column(name = "sender_address")
    var senderAddress: String? = null,

    @Column(name = "recipient_address")
    var recipientAddress: String? = null,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    /** Network fee paid for this transaction in nanotons. Null for incoming transactions (fee paid by sender). */
    @Column(name = "fee_nano")
    var feeNano: Long? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now()
)

enum class TransactionDirection {
    INCOMING,
    OUTGOING
}
