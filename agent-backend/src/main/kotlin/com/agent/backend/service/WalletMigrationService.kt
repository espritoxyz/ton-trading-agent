package com.agent.backend.service

import com.agent.backend.db.rep.AgentUserRepository
import com.agent.backend.db.rep.UserWalletRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlinx.coroutines.*

@Service
class WalletMigrationService(
    private val userRepository: AgentUserRepository,
    private val userWalletRepository: UserWalletRepository,
    private val walletService: WalletService
) {
    private val logger = LoggerFactory.getLogger(WalletMigrationService::class.java)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Migrate all existing users to burner wallets
     * Processes in batches to avoid overloading the recipe-processor
     */
    @Transactional(readOnly = true)
    fun migrateExistingUsers(): MigrationResult {
        logger.info("[wallet-migration] Starting user wallet migration")

        // Find users without wallets
        val allUsers = userRepository.findAll()
        val usersWithWallets = userWalletRepository.findAll().map { it.userId }.toSet()
        val usersToMigrate = allUsers.filter { it.id !in usersWithWallets }

        if (usersToMigrate.isEmpty()) {
            logger.info("[wallet-migration] No users to migrate")
            return MigrationResult(
                totalUsers = allUsers.size,
                alreadyMigrated = usersWithWallets.size,
                toMigrate = 0,
                initiated = 0
            )
        }

        logger.info("[wallet-migration] Found ${usersToMigrate.size} users to migrate")

        // Process in batches
        val batchSize = 10
        val delayBetweenBatches = 1000L // 1 second

        var initiatedCount = 0
        usersToMigrate.chunked(batchSize).forEachIndexed { index, batch ->
            logger.info("[wallet-migration] Processing batch ${index + 1}/${(usersToMigrate.size + batchSize - 1) / batchSize}")

            batch.forEach { user ->
                try {
                    walletService.createWalletForUser(user.id!!)
                    initiatedCount++
                    logger.info("[wallet-migration] Initiated wallet creation for user ${user.id}")
                } catch (e: Exception) {
                    logger.error("[wallet-migration] Error initiating wallet creation for user ${user.id}", e)
                }
            }

            // Delay between batches (except for last batch)
            if (index < usersToMigrate.size / batchSize) {
                Thread.sleep(delayBetweenBatches)
            }
        }

        logger.info("[wallet-migration] Migration initiated for $initiatedCount users")

        return MigrationResult(
            totalUsers = allUsers.size,
            alreadyMigrated = usersWithWallets.size,
            toMigrate = usersToMigrate.size,
            initiated = initiatedCount
        )
    }

    /**
     * Get migration status
     */
    @Transactional(readOnly = true)
    fun getMigrationStatus(): MigrationStatus {
        val totalUsers = userRepository.count()
        val usersWithWallets = userWalletRepository.count()
        val usersWithoutWallets = totalUsers - usersWithWallets

        return MigrationStatus(
            totalUsers = totalUsers,
            migratedUsers = usersWithWallets,
            pendingUsers = usersWithoutWallets,
            migrationPercentage = if (totalUsers > 0) {
                (usersWithWallets.toDouble() / totalUsers * 100).toInt()
            } else {
                100
            }
        )
    }
}

data class MigrationResult(
    val totalUsers: Int,
    val alreadyMigrated: Int,
    val toMigrate: Int,
    val initiated: Int
)

data class MigrationStatus(
    val totalUsers: Long,
    val migratedUsers: Long,
    val pendingUsers: Long,
    val migrationPercentage: Int
)
