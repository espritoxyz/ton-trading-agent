import { TonApiClient } from '@ton-api/client';
import { Address } from "@ton/core";
import { sleep } from "../utils.js";
import type { Channel } from "amqplib";

const TONAPI_BASE_URL = process.env.TONAPI_BASE_URL;
const TONAPI_KEY = process.env.TONAPI_KEY;
const POLL_INTERVAL_MS = parseInt(process.env.MULTI_WALLET_POLL_INTERVAL_MS || "12000");
const BATCH_SIZE = 100; // TonAPI supports up to 100 addresses per request

interface WalletMonitorState {
    userId: number;
    walletAddress: string;
    lastProcessedLt: bigint;
}

interface MonitoredWallet {
    userId: number;
    walletAddress: string;
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

    console.log("[multi-wallet-monitor] Starting multi-wallet monitoring...");
    console.log("[multi-wallet-monitor] TonAPI endpoint:", TONAPI_BASE_URL);
    console.log("[multi-wallet-monitor] Polling interval:", POLL_INTERVAL_MS, "ms");
    console.log("[multi-wallet-monitor] Batch size:", BATCH_SIZE);

    // Request initial list of active wallets
    await requestActiveWallets(rabbitChannel, rabbitExchange);
    await sleep(2000); // Give backend time to respond

    while (isMonitoring) {
        try {
            if (monitoredWallets.size > 0) {
                await pollAllWallets(client, rabbitChannel, rabbitExchange);
                consecutiveErrors = 0;
            } else {
                console.log("[multi-wallet-monitor] No wallets to monitor yet, waiting...");
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
 * Request list of active wallets from backend
 */
async function requestActiveWallets(channel: Channel, exchange: string) {
    const message = {
        type: "wallet.list-active-request",
        occurredAt: new Date().toISOString(),
        data: {}
    };

    channel.publish(
        exchange,
        "wallet.list-active-request",
        Buffer.from(JSON.stringify(message)),
        { persistent: true }
    );

    console.log("[multi-wallet-monitor] Requested active wallets list");
}

/**
 * Add wallet to monitoring
 */
export function addWalletToMonitor(walletAddress: string, userId: number) {
    if (!monitoredWallets.has(walletAddress)) {
        monitoredWallets.set(walletAddress, {
            userId,
            walletAddress,
            lastProcessedLt: BigInt(0)
        });
        console.log(`[multi-wallet-monitor] Added wallet to monitoring: ${walletAddress} (user ${userId})`);
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
 * Update monitored wallets list
 */
export function updateMonitoredWallets(wallets: MonitoredWallet[]) {
    console.log(`[multi-wallet-monitor] Updating monitored wallets list: ${wallets.length} wallets`);

    // Add new wallets
    for (const wallet of wallets) {
        addWalletToMonitor(wallet.walletAddress, wallet.userId);
    }

    // Remove wallets that are no longer in the list
    const activeAddresses = new Set(wallets.map(w => w.walletAddress));
    for (const address of monitoredWallets.keys()) {
        if (!activeAddresses.has(address)) {
            removeWalletFromMonitor(address);
        }
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
            limit: 20,
            start_date: wallet.lastProcessedLt > 0 ? Number(wallet.lastProcessedLt) : undefined
        });

        if (!events.events || events.events.length === 0) {
            return;
        }

        // Process events in chronological order (oldest first)
        const sortedEvents = [...events.events].reverse();

        for (const event of sortedEvents) {
            const eventLt = BigInt(event.lt);

            // Skip already processed events
            if (eventLt <= wallet.lastProcessedLt) {
                continue;
            }

            // Process different event types
            await processEvent(event, wallet, channel, exchange);

            // Update last processed LT
            wallet.lastProcessedLt = eventLt;
            monitoredWallets.set(wallet.walletAddress, wallet);
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
        // Check for TON transfers
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
    if (!transfer) return;

    const recipient = transfer.recipient?.address;
    const sender = transfer.sender?.address;

    // Only process incoming transfers
    if (recipient !== wallet.walletAddress) {
        return;
    }

    const amountNano = transfer.amount;
    const comment = transfer.comment || "";

    console.log(`[multi-wallet-monitor] TON transfer to ${wallet.walletAddress} (user ${wallet.userId}): ${amountNano} nano`);

    const message = {
        type: "wallet.transaction-detected",
        occurredAt: new Date().toISOString(),
        data: {
            userId: wallet.userId,
            walletAddress: wallet.walletAddress,
            transactionHash: event.event_id,
            transactionLt: event.lt,
            amountNano: amountNano.toString(),
            assetType: "TON",
            senderAddress: sender,
            comment: comment || undefined
        }
    };

    channel.publish(
        exchange,
        "wallet.transaction-detected",
        Buffer.from(JSON.stringify(message)),
        { persistent: true }
    );

    console.log(`[multi-wallet-monitor] Published TON transaction for user ${wallet.userId}`);
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
    if (!transfer) return;

    const recipient = transfer.recipient?.address;
    const sender = transfer.sender?.address;

    // Only process incoming transfers
    if (recipient !== wallet.walletAddress) {
        return;
    }

    const amountNano = transfer.amount;
    const jetton = transfer.jetton;
    const comment = transfer.comment || "";

    console.log(`[multi-wallet-monitor] Jetton transfer to ${wallet.walletAddress} (user ${wallet.userId}): ${amountNano} ${jetton?.symbol || 'tokens'}`);

    const message = {
        type: "wallet.transaction-detected",
        occurredAt: new Date().toISOString(),
        data: {
            userId: wallet.userId,
            walletAddress: wallet.walletAddress,
            transactionHash: event.event_id,
            transactionLt: event.lt,
            amountNano: amountNano.toString(),
            assetType: "JETTON",
            senderAddress: sender,
            jettonMasterAddress: jetton?.address,
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

    console.log(`[multi-wallet-monitor] Published Jetton transaction for user ${wallet.userId}`);
}
