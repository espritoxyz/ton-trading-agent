package com.agent.backend.controller

import com.agent.backend.service.WalletMigrationService
import com.agent.backend.service.MigrationResult
import com.agent.backend.service.MigrationStatus
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

private val adminLogger = KotlinLogging.logger {}

@RestController
@RequestMapping("/admin")
class AdminController(
    private val walletMigrationService: WalletMigrationService
) {

    /**
     * Migrate all existing users to burner wallets
     * Protected endpoint - requires admin role
     */
    @PostMapping("/migrate-wallets")
    @PreAuthorize("hasRole('ADMIN')")
    fun migrateWallets(): ResponseEntity<MigrationResult> {
        adminLogger.info { "Admin wallet migration requested" }

        val result = walletMigrationService.migrateExistingUsers()

        adminLogger.info { "Wallet migration completed: ${result.initiated} users initiated" }

        return ResponseEntity.ok(result)
    }

    /**
     * Get wallet migration status
     */
    @GetMapping("/migration-status")
    @PreAuthorize("hasRole('ADMIN')")
    fun getMigrationStatus(): ResponseEntity<MigrationStatus> {
        val status = walletMigrationService.getMigrationStatus()
        return ResponseEntity.ok(status)
    }
}
