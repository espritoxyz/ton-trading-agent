import { TonApiClient } from '@ton-api/client';
import { Address } from "@ton/core";
import { sleep } from "../utils.js";
import type { Channel } from "amqplib";
import { syncWalletBalance } from "./walletBalanceSync.js";

const TONAPI_BASE_URL = process.env.TONAPI_BASE_URL;
const TONAPI_KEY = process.env.TONAPI_KEY;
const POLL_INTERVAL_MS = parseInt(process.env.MULTI_WALLET_POLL_INTERVAL_MS || "12000");
const BATCH_SIZE = 100; // TonAPI supports up to 100 addresses per request
const CLEANUP_INTERVAL_MS = 60000; // Clean up expired wallets every minute

interface WalletMonitorState {
    userId: number;
    walletAddress: string;
    lastProcessedLt: bigint;
    expiresAt: Date;
    processedTxHashes: Set<string>; // Track processed transaction hashes to prevent duplicates
}

const monitoredWallets = new Map<string, WalletMonitorState>();
let isMonitoring = false;
let consecutiveErrors = 0;

/**
 * Start multi-wallet monitoring
 */
export async function startMultiWalletMonitoring(rabbitChannel: Channel, rabbitExchange: string) {
    if (isMonitoring) {
        console.log("[multi-wallet-monitor] Already monitoring, skipping duplicate start");
        return;
    }

    isMonitoring = true;

    const client = new TonApiClient({
        baseUrl: TONAPI_BASE_URL,
        apiKey: TONAPI_KEY,
    });

    console.log("[multi-wallet-monitor] Starting session-based multi-wallet monitoring...");
    console.log("[multi-wallet-monitor] TonAPI endpoint:", TONAPI_BASE_URL);
    console.log("[multi-wallet-monitor] Polling interval:", POLL_INTERVAL_MS, "ms");
    console.log("[multi-wallet-monitor] Cleanup interval:", CLEANUP_INTERVAL_MS, "ms");
    console.log("[multi-wallet-monitor] Batch size:", BATCH_SIZE);

    // Start cleanup task
    startCleanupTask();

    while (isMonitoring) {
        try {
            if (monitoredWallets.size > 0) {
                console.log(`[multi-wallet-monitor] Polling ${monitoredWallets.size} active deposit sessions...`);
                await pollAllWallets(client, rabbitChannel, rabbitExchange);
                consecutiveErrors = 0;
            } else {
                console.log("[multi-wallet-monitor] No active deposit sessions, waiting...");
            }
        } catch (error: any) {
            consecutiveErrors++;
            const errorMsg = error?.message || String(error);
            console.error(`[multi-wallet-monitor] Error polling events (attempt ${consecutiveErrors}):`, errorMsg);

            if (consecutiveErrors >= 10) {
                console.error("[multi-wallet-monitor] Too many consecutive errors, waiting longer...");
                await sleep(60000);
            }
        }

        await sleep(POLL_INTERVAL_MS);
    }
}

/**
 * Start periodic cleanup of expired wallets
 */
async function startCleanupTask() {
    while (isMonitoring) {
        await sleep(CLEANUP_INTERVAL_MS);
        cleanupExpiredWallets();
        cleanupOldTransactionHashes();
    }
}

/**
 * Remove expired wallets from monitoring
 */
function cleanupExpiredWallets() {
    const now = new Date();
    const expiredWallets: string[] = [];

    for (const [address, state] of monitoredWallets.entries()) {
        if (state.expiresAt < now) {
            expiredWallets.push(address);
        }
    }

    if (expiredWallets.length > 0) {
        for (const address of expiredWallets) {
            monitoredWallets.delete(address);
        }
        console.log(`[multi-wallet-monitor] Cleaned up ${expiredWallets.length} expired deposit sessions`);
    }
}

/**
 * Clean up old transaction hashes to prevent memory bloat
 * Keep only the most recent 1000 hashes per wallet
 */
function cleanupOldTransactionHashes() {
    const MAX_TX_HASHES = 1000;
    let totalCleaned = 0;

    for (const [address, state] of monitoredWallets.entries()) {
        if (state.processedTxHashes.size > MAX_TX_HASHES) {
            const oldSize = state.processedTxHashes.size;
            // Clear the entire set if it grows too large
            // This is safe because duplicates within a short time window are already prevented by lt tracking
            state.processedTxHashes.clear();
            totalCleaned += oldSize;
            console.log(`[multi-wallet-monitor] Cleared ${oldSize} old transaction hashes for wallet ${address}`);
        }
    }

    if (totalCleaned > 0) {
        console.log(`[multi-wallet-monitor] Total transaction hashes cleaned: ${totalCleaned}`);
    }
}

/**
 * Add wallet to monitoring with TTL
 */
export function addWalletToMonitor(walletAddress: string, userId: number, expiresAt: Date) {
    const existing = monitoredWallets.get(walletAddress);

    if (existing) {
        // Update expiration if extending session
        if (expiresAt > existing.expiresAt) {
            existing.expiresAt = expiresAt;
            monitoredWallets.set(walletAddress, existing);
            console.log(`[multi-wallet-monitor] Extended monitoring session for ${walletAddress} until ${expiresAt.toISOString()}`);
        }
    } else {
        monitoredWallets.set(walletAddress, {
            userId,
            walletAddress,
            lastProcessedLt: BigInt(0),
            expiresAt,
            processedTxHashes: new Set<string>()
        });
        console.log(`[multi-wallet-monitor] Added wallet to monitoring: ${walletAddress} (user ${userId}) until ${expiresAt.toISOString()}`);
    }
}

/**
 * Remove wallet from monitoring
 */
export function removeWalletFromMonitor(walletAddress: string) {
    if (monitoredWallets.delete(walletAddress)) {
        console.log(`[multi-wallet-monitor] Removed wallet from monitoring: ${walletAddress}`);
    }
}

/**
 * Poll all monitored wallets in batches
 */
async function pollAllWallets(client: TonApiClient, channel: Channel, exchange: string) {
    const wallets = Array.from(monitoredWallets.values());

    // Process in batches
    for (let i = 0; i < wallets.length; i += BATCH_SIZE) {
        const batch = wallets.slice(i, i + BATCH_SIZE);
        await pollWalletBatch(client, batch, channel, exchange);
    }
}

/**
 * Poll a batch of wallets for new transactions
 */
async function pollWalletBatch(
    client: TonApiClient,
    wallets: WalletMonitorState[],
    channel: Channel,
    exchange: string
) {
    try {
        // Get events for each wallet in the batch
        for (const wallet of wallets) {
            try {
                await pollWalletEvents(client, wallet, channel, exchange);
            } catch (error) {
                console.error(`[multi-wallet-monitor] Error polling wallet ${wallet.walletAddress}:`, error);
            }
        }
    } catch (error) {
        console.error("[multi-wallet-monitor] Error polling wallet batch:", error);
        throw error;
    }
}

/**
 * Poll events for a single wallet
 */
async function pollWalletEvents(
    client: TonApiClient,
    wallet: WalletMonitorState,
    channel: Channel,
    exchange: string
) {
    try {
        const events = await client.accounts.getAccountEvents(Address.parse(wallet.walletAddress), {
            limit: 20
        });

        if (!events.events || events.events.length === 0) {
            return;
        }

        // Process events in chronological order (oldest first)
        const sortedEvents = [...events.events].reverse();
        let newEventsCount = 0;

        for (const event of sortedEvents) {
            const eventLt = BigInt(event.lt);

            // Skip already processed events
            if (eventLt <= wallet.lastProcessedLt) {
                continue;
            }

            // Skip events that are still being processed (incomplete transactions)
            if (event.inProgress) {
                console.log(`[multi-wallet-monitor] Skipping in-progress event: lt=${eventLt}, event_id=${event.eventId}`);
                continue;
            }

            newEventsCount++;
            console.log(`[multi-wallet-monitor] Processing new event: lt=${eventLt}, event_id=${event.eventId}, actions=${event.actions?.length || 0}`);

            // Process different event types
            await processEvent(event, wallet, channel, exchange);

            // Update last processed LT
            wallet.lastProcessedLt = eventLt;
            monitoredWallets.set(wallet.walletAddress, wallet);
        }

        if (newEventsCount > 0) {
            console.log(`[multi-wallet-monitor] Processed ${newEventsCount} new events for wallet ${wallet.walletAddress} (user ${wallet.userId})`);
        }
    } catch (error: any) {
        // Log but don't throw - we don't want one wallet failure to stop monitoring
        console.error(`[multi-wallet-monitor] Error fetching events for ${wallet.walletAddress}:`, error?.message);
    }
}

/**
 * Process a single event
 */
async function processEvent(
    event: any,
    wallet: WalletMonitorState,
    channel: Channel,
    exchange: string
) {
    try {
        // Check for TON and Jetton transfers
        for (const action of event.actions || []) {
            if (action.type === "TonTransfer") {
                await processTonTransfer(action, event, wallet, channel, exchange);
            } else if (action.type === "JettonTransfer") {
                await processJettonTransfer(action, event, wallet, channel, exchange);
            }
        }
    } catch (error) {
        console.error(`[multi-wallet-monitor] Error processing event:`, error);
    }
}

/**
 * Process TON transfer
 */
async function processTonTransfer(
    action: any,
    event: any,
    wallet: WalletMonitorState,
    channel: Channel,
    exchange: string
) {
    const transfer = action.TonTransfer;
    if (!transfer) {
        console.log(`[multi-wallet-monitor] TonTransfer action has no transfer data`);
        return;
    }

    const recipient = transfer.recipient?.address;
    const sender = transfer.sender?.address;

    // Only process incoming transfers - use Address.equals() for proper comparison
    try {
        // Ensure we have strings for Address.parse()
        const recipientStr = typeof recipient === 'string' ? recipient : String(recipient);
        const recipientAddr = Address.parse(recipientStr);
        const walletAddr = Address.parse(wallet.walletAddress);

        if (!recipientAddr.equals(walletAddr)) {
            console.log(`[multi-wallet-monitor] Skipping TonTransfer: not incoming (recipient=${recipient}, expected=${wallet.walletAddress})`);
            return;
        }
    } catch (error) {
        console.error(`[multi-wallet-monitor] Error parsing addresses for comparison:`, error);
        return;
    }

    const amountNano = transfer.amount;
    const comment = transfer.comment || "";

    // Use baseTransactions to get unique transaction hash for this specific action
    // baseTransactions is an array of transaction hashes involved in this action
    const transactionHash = action.baseTransactions?.[0] || event.eventId || `${wallet.walletAddress}:${event.lt}`;

    // Check if we already processed this transaction hash
    if (wallet.processedTxHashes.has(transactionHash)) {
        console.log(`[multi-wallet-monitor] ⏭️ Skipping duplicate TON transaction: ${transactionHash} (already processed)`);
        return;
    }

    console.log(`[multi-wallet-monitor] TON deposit: ${amountNano} nano to user ${wallet.userId}, txHash: ${transactionHash}`);

    const message = {
        type: "wallet.transaction-detected",
        occurredAt: new Date().toISOString(),
        data: {
            userId: wallet.userId,
            walletAddress: wallet.walletAddress,
            transactionHash: transactionHash,
            transactionLt: String(event.lt), // Convert BigInt to string
            amountNano: String(amountNano), // Convert BigInt to string
            assetType: "TON",
            senderAddress: String(sender), // Ensure string
            comment: comment || undefined
        }
    };

    channel.publish(
        exchange,
        "wallet.transaction-detected",
        Buffer.from(JSON.stringify(message)),
        { persistent: true }
    );

    // Mark transaction as processed to prevent duplicates
    wallet.processedTxHashes.add(transactionHash);
    monitoredWallets.set(wallet.walletAddress, wallet);

    // Sync wallet balance after incoming TON transaction
    try {
        await syncWalletBalance(wallet.walletAddress, wallet.userId, channel, exchange);
    } catch (syncErr: any) {
        console.error(`[multi-wallet-monitor] Failed to sync wallet balance after TON deposit:`, syncErr?.message);
        // Don't fail the operation if sync fails
    }
}

/**
 * Process Jetton transfer
 */
async function processJettonTransfer(
    action: any,
    event: any,
    wallet: WalletMonitorState,
    channel: Channel,
    exchange: string
) {
    const transfer = action.JettonTransfer;
    if (!transfer) {
        return;
    }

    const recipient = transfer.recipient?.address;
    const sender = transfer.sender?.address;

    // Only process incoming transfers - use Address.equals() for proper comparison
    try {
        // Ensure we have strings for Address.parse()
        const recipientStr = typeof recipient === 'string' ? recipient : String(recipient);
        const recipientAddr = Address.parse(recipientStr);
        const walletAddr = Address.parse(wallet.walletAddress);

        if (!recipientAddr.equals(walletAddr)) {
            return;
        }
    } catch (error) {
        console.error(`[multi-wallet-monitor] Error parsing addresses for comparison:`, error);
        return;
    }

    const amountNano = transfer.amount;
    const jetton = transfer.jetton;
    const comment = transfer.comment || "";

    // Use baseTransactions to get unique transaction hash for this specific action
    // baseTransactions is an array of transaction hashes involved in this action
    const transactionHash = action.baseTransactions?.[0] || event.eventId || `${wallet.walletAddress}:${event.lt}`;

    // Check if we already processed this transaction hash
    if (wallet.processedTxHashes.has(transactionHash)) {
        console.log(`[multi-wallet-monitor] ⏭️ Skipping duplicate Jetton transaction: ${transactionHash} (already processed)`);
        return;
    }

    console.log(`[multi-wallet-monitor] ✅ Jetton deposit: ${amountNano} ${jetton?.symbol || 'tokens'} to user ${wallet.userId}, txHash: ${transactionHash}`);

    const message = {
        type: "wallet.transaction-detected",
        occurredAt: new Date().toISOString(),
        data: {
            userId: wallet.userId,
            walletAddress: wallet.walletAddress,
            transactionHash: transactionHash,
            transactionLt: String(event.lt), // Convert BigInt to string
            amountNano: String(amountNano), // Convert BigInt to string
            assetType: "JETTON",
            senderAddress: String(sender), // Ensure string
            jettonMasterAddress: jetton?.address ? String(jetton.address) : undefined,
            jettonSymbol: jetton?.symbol,
            jettonDecimals: jetton?.decimals,
            comment: comment || undefined
        }
    };

    channel.publish(
        exchange,
        "wallet.transaction-detected",
        Buffer.from(JSON.stringify(message)),
        { persistent: true }
    );

    // Mark transaction as processed to prevent duplicates
    wallet.processedTxHashes.add(transactionHash);
    monitoredWallets.set(wallet.walletAddress, wallet);

    // Sync wallet balance after incoming Jetton transaction
    try {
        await syncWalletBalance(wallet.walletAddress, wallet.userId, channel, exchange);
    } catch (syncErr: any) {
        console.error(`[multi-wallet-monitor] Failed to sync wallet balance after Jetton deposit:`, syncErr?.message);
        // Don't fail the operation if sync fails
    }
}
