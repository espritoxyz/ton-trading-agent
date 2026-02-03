import { TonClient, Address } from "@ton/ton";
import { mnemonicToPrivateKey } from "@ton/crypto";
import { WalletContractV5R1 } from "@ton/ton";
import { mnemonic_array } from "../mnemonics.js";
import { parseDepositComment } from "../commentParser.js";
import { sleep, bufToHex } from "../utils.js";
import type { Channel } from "amqplib";

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";
const POLL_INTERVAL_MS = parseInt(process.env.DEPOSIT_POLL_INTERVAL_MS || "8000");
const MAX_RETRIES = 5;
const INITIAL_RETRY_DELAY = 1000;

let lastLt: bigint = BigInt(0);
let isMonitoring = false;
let consecutiveErrors = 0;

export async function startDepositMonitoring(rabbitChannel: Channel, rabbitExchange: string) {
    if (isMonitoring) {
        console.log("[deposit-monitor] Already monitoring, skipping duplicate start");
        return;
    }

    isMonitoring = true;

    const client = new TonClient({
        endpoint,
        apiKey,
        timeout: 30000, // 30 second timeout
    });
    const { publicKey } = await mnemonicToPrivateKey(mnemonic_array);
    const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
    const walletAddress = wallet.address.toString({ bounceable: false });

    console.log("[deposit-monitor] Starting deposit monitoring...");
    console.log("[deposit-monitor] Endpoint:", endpoint);
    console.log("[deposit-monitor] Deposit wallet address:", walletAddress);
    console.log("[deposit-monitor] Polling interval:", POLL_INTERVAL_MS, "ms");

    while (isMonitoring) {
        try {
            await pollTransactionsWithRetry(client, wallet.address, rabbitChannel, rabbitExchange);
            consecutiveErrors = 0; // Reset error counter on success
        } catch (error: any) {
            consecutiveErrors++;
            const errorMsg = error?.message || String(error);
            console.error(`[deposit-monitor] Error polling transactions (attempt ${consecutiveErrors}):`, errorMsg);

            if (consecutiveErrors >= 10) {
                console.error("[deposit-monitor] Too many consecutive errors, waiting longer before retry...");
                await sleep(60000); // Wait 1 minute after many errors
            }
        }

        await sleep(POLL_INTERVAL_MS);
    }
}

async function pollTransactionsWithRetry(
    client: TonClient,
    walletAddress: Address,
    rabbitChannel: Channel,
    rabbitExchange: string
) {
    let lastError: Error | null = null;

    for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
        try {
            await pollTransactions(client, walletAddress, rabbitChannel, rabbitExchange);
            return; // Success
        } catch (error: any) {
            lastError = error;

            // Check if it's a network error
            const isNetworkError =
                error?.code === 'EAI_AGAIN' ||
                error?.code === 'ENOTFOUND' ||
                error?.code === 'ETIMEDOUT' ||
                error?.code === 'ECONNREFUSED' ||
                error?.message?.includes('getaddrinfo') ||
                error?.message?.includes('network');

            if (!isNetworkError) {
                // If it's not a network error, don't retry
                throw error;
            }

            if (attempt < MAX_RETRIES - 1) {
                const delay = INITIAL_RETRY_DELAY * Math.pow(2, attempt);
                console.log(`[deposit-monitor] Network error, retrying in ${delay}ms (attempt ${attempt + 1}/${MAX_RETRIES})...`);
                await sleep(delay);
            }
        }
    }

    // All retries failed
    throw lastError || new Error('Failed after all retries');
}

async function pollTransactions(
    client: TonClient,
    walletAddress: Address,
    rabbitChannel: Channel,
    rabbitExchange: string
) {
    const startTime = Date.now();
    try {
        const transactions = await client.getTransactions(walletAddress, { limit: 20 });
        const duration = Date.now() - startTime;

        if (transactions.length === 0) {
            console.log(`[deposit-monitor] No transactions found (${duration}ms)`);
            return;
        }

        console.log(`[deposit-monitor] Fetched ${transactions.length} transactions (${duration}ms)`);

        // Process transactions in chronological order (oldest first)
        const sortedTxs = transactions
            .filter((tx) => tx.inMessage && tx.inMessage.info.type === "internal")
            .sort((a, b) => {
                const aLt = BigInt(a.lt);
                const bLt = BigInt(b.lt);
                return aLt < bLt ? -1 : aLt > bLt ? 1 : 0;
            });

        let newTxCount = 0;

        for (const tx of sortedTxs) {
            const txLt = BigInt(tx.lt);

            // Skip if we've already processed this transaction
            if (txLt <= lastLt) {
                continue;
            }

            const inMsg = tx.inMessage;
            if (!inMsg || inMsg.info.type !== "internal") {
                continue;
            }

            // Skip bounced messages
            if (inMsg.info.bounced) {
                console.log(`[deposit-monitor] Skipping bounced transaction, lt=${tx.lt}`);
                continue;
            }

            // Check if transaction has value (skip zero-value notifications)
            const txValue = inMsg.info.value.coins;
            if (txValue === 0n) {
                console.log(`[deposit-monitor] Skipping zero-value transaction, lt=${tx.lt}`);
                continue;
            }

            // Extract transaction details
            const sender = inMsg.info.src.toString({ bounceable: false });

            // Get amount in nanotons - inMsg.info.value.coins is a Coins object (bigint)
            const amountNano = inMsg.info.value.coins.toString();
            const amountTon = (Number(inMsg.info.value.coins) / 1_000_000_000).toFixed(4);

            const txHash = bufToHex(tx.hash());

            // Compute body hash for duplicate detection
            const bodyHash = inMsg.body ? bufToHex(inMsg.body.hash()) : txHash;

            // Parse comment from transaction body
            const comment = parseDepositComment(inMsg.body);

            console.log(`[deposit-monitor] New incoming transaction:`, {
                lt: tx.lt.toString(),
                hash: txHash,
                from: sender,
                amount: `${amountTon} TON (${amountNano} nano)`,
                comment: comment || "(no valid deposit code)",
                hasBody: !!inMsg.body
            });

            // If there's a valid deposit code, publish event to backend
            if (comment) {
                const depositData = {
                    transactionHash: txHash,
                    transactionLt: tx.lt.toString(),
                    bodyHash,
                    comment,
                    amountTonNano: amountNano,
                    sender,
                };

                console.log(`[deposit-monitor] ✅ Valid deposit code found: ${comment}`);
                console.log(`[deposit-monitor] Amount: ${amountTon} TON, Sender: ${sender}`);

                try {
                    await publishDepositTransaction(rabbitChannel, rabbitExchange, depositData);
                    console.log(`[deposit-monitor] 📤 Published deposit event to backend`);
                } catch (publishError) {
                    console.error(`[deposit-monitor] ❌ Failed to publish deposit event:`, publishError);
                    // Don't throw - we'll try again on next poll if needed
                }
            } else {
                // Log transactions without valid deposit codes at debug level
                console.log(`[deposit-monitor] Transaction without deposit code (normal transfer or other operation)`);
            }

            newTxCount++;
            lastLt = txLt;
        }

        if (newTxCount > 0) {
            console.log(`[deposit-monitor] Processed ${newTxCount} new transaction(s), lastLt=${lastLt}`);
        }
    } catch (error) {
        console.error("[deposit-monitor] Error fetching transactions:", error);
        throw error;
    }
}

async function publishDepositTransaction(
    channel: Channel,
    exchange: string,
    data: {
        transactionHash: string;
        transactionLt: string;
        bodyHash: string;
        comment: string;
        amountTonNano: string;
        sender: string;
    }
) {
    const event = {
        type: "deposit.transaction-found",
        occurredAt: new Date().toISOString(),
        data,
    };

    const routingKey = "deposit.transaction-found";

    console.log(`[deposit-monitor] Publishing event:`, {
        type: event.type,
        code: data.comment,
        amount: data.amountTonNano,
        txHash: data.transactionHash.substring(0, 16) + "...",
    });

    channel.publish(exchange, routingKey, Buffer.from(JSON.stringify(event)), {
        persistent: true,
        contentType: "application/json",
    });
}

export function stopDepositMonitoring() {
    isMonitoring = false;
    console.log("[deposit-monitor] Stopping deposit monitoring...");
}
