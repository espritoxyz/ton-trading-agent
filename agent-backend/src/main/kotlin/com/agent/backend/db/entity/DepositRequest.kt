package com.agent.backend.db.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "deposit_request",
    indexes = [
        Index(name = "idx_deposit_request_code", columnList = "code"),
        Index(name = "idx_deposit_request_user_id", columnList = "user_id"),
        Index(name = "idx_deposit_request_status", columnList = "status")
    ]
)
class DepositRequest(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, unique = true, length = 6)
    var code: String,

    @Column(name = "deposit_wallet_address", nullable = false, length = 100)
    var depositWalletAddress: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: DepositStatus = DepositStatus.PENDING,

    @Column(name = "amount_nano")
    var amountNano: Long? = null,

    @Column(name = "asset_type", length = 20)
    var assetType: String? = null, // "TON" or "JETTON"

    @Column(name = "jetton_master_address", length = 100)
    var jettonMasterAddress: String? = null,

    @Column(name = "jetton_symbol", length = 20)
    var jettonSymbol: String? = null,

    @Column(name = "jetton_decimals")
    var jettonDecimals: Int? = null,

    @Column(name = "transaction_hash", length = 64)
    var transactionHash: String? = null,

    @Column(name = "transaction_lt")
    var transactionLt: Long? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = Instant.now(),

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(name = "completed_at")
    var completedAt: Instant? = null
)

enum class DepositStatus {
    PENDING,
    COMPLETED,
    EXPIRED,
    CANCELLED
}
