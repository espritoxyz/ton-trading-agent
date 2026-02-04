import {TonApiClient} from '@ton-api/client';
import {Address} from "@ton/core";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {WalletContractV5R1} from "@ton/ton";
import {mnemonic_array} from "../mnemonics.js";
import {sleep} from "../utils.js";
import type {Channel} from "amqplib";

const TONAPI_BASE_URL = process.env.TONAPI_BASE_URL;
const TONAPI_KEY = process.env.TONAPI_KEY;
const POLL_INTERVAL_MS = parseInt(process.env.DEPOSIT_POLL_INTERVAL_MS || "8000");
const MAX_RETRIES = 5;
const INITIAL_RETRY_DELAY = 1000;

let lastProcessedLt: bigint = BigInt(0); // Track last processed logical time
let isMonitoring = false;
let consecutiveErrors = 0;

export async function startDepositMonitoring(rabbitChannel: Channel, rabbitExchange: string) {
    if (isMonitoring) {
        console.log("[deposit-monitor] Already monitoring, skipping duplicate start");
        return;
    }

    isMonitoring = true;

    const client = new TonApiClient({
        baseUrl: TONAPI_BASE_URL,
        apiKey: TONAPI_KEY,
    });

    const {publicKey} = await mnemonicToPrivateKey(mnemonic_array);
    const wallet = WalletContractV5R1.create({publicKey, workchain: 0});
    const walletAddress = wallet.address.toString({bounceable: false});

    console.log("[deposit-monitor] Starting deposit monitoring...");
    console.log("[deposit-monitor] TonAPI endpoint:", TONAPI_BASE_URL);
    console.log("[deposit-monitor] Deposit wallet address:", walletAddress);
    console.log("[deposit-monitor] Polling interval:", POLL_INTERVAL_MS, "ms");

    while (isMonitoring) {
        try {
            await pollEventsWithRetry(client, walletAddress, rabbitChannel, rabbitExchange);
            consecutiveErrors = 0; // Reset error counter on success
        } catch (error: any) {
            consecutiveErrors++;
            const errorMsg = error?.message || String(error);
            console.error(`[deposit-monitor] Error polling events (attempt ${consecutiveErrors}):`, errorMsg);

            if (consecutiveErrors >= 10) {
                console.error("[deposit-monitor] Too many consecutive errors, waiting longer before retry...");
                await sleep(60000); // Wait 1 minute after many errors
            }
        }

        await sleep(POLL_INTERVAL_MS);
    }
}

async function pollEventsWithRetry(
    client: TonApiClient,
    walletAddress: string,
    rabbitChannel: Channel,
    rabbitExchange: string
) {
    let lastError: Error | null = null;

    for (let attempt = 0; attempt < MAX_RETRIES; attempt++) {
        try {
            await pollEvents(client, walletAddress, rabbitChannel, rabbitExchange);
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

async function pollEvents(
    client: TonApiClient,
    walletAddress: string,
    rabbitChannel: Channel,
    rabbitExchange: string
) {
    const startTime = Date.now();
    try {
        // Fetch recent account events (includes both TON and Jetton transfers)
        const params: any = {limit: 20};

        const eventsResponse = await client.accounts.getAccountEvents(Address.parse(walletAddress), params);
        const duration = Date.now() - startTime;

        if (!eventsResponse.events || eventsResponse.events.length === 0) {
            console.log(`[deposit-monitor] No new events found (${duration}ms), lastProcessedLt=${lastProcessedLt.toString()}`);
            return;
        }

        console.debug(`[deposit-monitor] Fetched ${eventsResponse.events.length} events (${duration}ms), lastProcessedLt=${lastProcessedLt.toString()}`);

        // Process events in chronological order (oldest first)
        const sortedEvents = [...eventsResponse.events].reverse();

        let newEventCount = 0;
        let maxLtSeen = lastProcessedLt;

        for (const event of sortedEvents) {
            const eventLt = event.lt || BigInt(0);

            // Skip if we've already processed this event (by logical time)
            if (eventLt <= lastProcessedLt) {
                continue;
            }

            // Track the highest lt we've seen
            if (eventLt > maxLtSeen) {
                maxLtSeen = eventLt;
            }

            console.log(`[deposit-monitor] Processing new event: lt=${eventLt.toString()}, eventId=${event.eventId}`);

            // Process actions in the event
            for (const action of event.actions) {
                const actionType = action.type;

                // Handle TON Transfer
                if (actionType === 'TonTransfer') {
                    const tonTransfer = action.TonTransfer;
                    if (!tonTransfer) continue;

                    // Check if this is an incoming transfer (recipient is our wallet)
                    const recipientAddr = tonTransfer.recipient?.address;
                    if (!recipientAddr || recipientAddr.toString({bounceable: false}) !== walletAddress) continue;

                    const senderAddr = tonTransfer.sender?.address;
                    const senderStr = senderAddr ? senderAddr.toString({bounceable: false}) : "unknown";
                    const amountNano = tonTransfer.amount?.toString() || "0";
                    const amountTon = (Number(amountNano) / 1_000_000_000).toFixed(4);

                    // Extract comment from the action
                    const comment = tonTransfer.comment || null;
                    const depositCode = comment ? validateDepositCode(comment) : null;

                    console.log(`[deposit-monitor] 📥 New TON transfer:`, {
                        eventId: event.eventId,
                        from: senderStr,
                        amount: `${amountTon} TON (${amountNano} nano)`,
                        comment: depositCode || "(no valid deposit code)",
                    });

                    if (depositCode) {
                        const depositData = {
                            transactionHash: event.eventId, // Use eventId as unique identifier
                            transactionLt: event.lt?.toString() || "0",
                            bodyHash: event.eventId, // Use eventId for duplicate detection
                            comment: depositCode,
                            amountNano: amountNano,
                            sender: senderStr,
                            assetType: "TON" as const,
                            jettonMasterAddress: null,
                            jettonSymbol: null,
                            jettonDecimals: null,
                        };

                        console.log(`[deposit-monitor] ✅ Valid TON deposit code found: ${depositCode}`);
                        console.log(`[deposit-monitor] Amount: ${amountTon} TON, Sender: ${senderStr}`);

                        try {
                            await publishDepositTransaction(rabbitChannel, rabbitExchange, depositData);
                            console.log(`[deposit-monitor] 📤 Published TON deposit event to backend`);
                        } catch (publishError) {
                            console.error(`[deposit-monitor] ❌ Failed to publish deposit event:`, publishError);
                        }
                    } else {
                        console.log(`[deposit-monitor] TON transfer without deposit code (normal transfer)`);
                    }
                }

                // Handle Jetton Transfer
                if (actionType === 'JettonTransfer') {
                    const jettonTransfer = action.JettonTransfer;
                    if (!jettonTransfer) continue;

                    // Check if this is an incoming transfer
                    const recipientAddr = jettonTransfer.recipient?.address;
                    if (!recipientAddr || recipientAddr.toString({bounceable: false}) !== walletAddress) continue;

                    const senderAddr = jettonTransfer.sender?.address;
                    const senderStr = senderAddr
                        ? (typeof senderAddr === 'string' ? senderAddr : senderAddr.toString({bounceable: false}))
                        : "unknown";

                    const amountBigInt = typeof jettonTransfer.amount === 'bigint'
                        ? jettonTransfer.amount
                        : BigInt(jettonTransfer.amount || "0");
                    const amountNano = amountBigInt.toString();

                    const jetton = jettonTransfer.jetton;

                    if (!jetton) {
                        console.log(`[deposit-monitor] ⚠️ Jetton transfer without jetton info, skipping`);
                        continue;
                    }

                    const jettonMasterAddr = jetton.address;
                    const jettonMasterAddress = typeof jettonMasterAddr === 'string'
                        ? jettonMasterAddr
                        : jettonMasterAddr.toString({bounceable: true});

                    const jettonSymbol = jetton.symbol || "UNKNOWN";
                    const jettonDecimals = jetton.decimals || 9;
                    const jettonName = jetton.name || jettonSymbol;

                    const amountReadable = (Number(amountBigInt) / Math.pow(10, jettonDecimals)).toFixed(jettonDecimals);

                    // Extract comment from the action
                    const comment = jettonTransfer.comment || null;
                    const depositCode = comment ? validateDepositCode(comment) : null;

                    console.log(`[deposit-monitor] 🪙 New Jetton transfer:`, {
                        eventId: event.eventId,
                        jetton: `${jettonName} (${jettonSymbol})`,
                        from: senderStr,
                        amount: `${amountReadable} ${jettonSymbol} (${amountNano} nano)`,
                        jettonMaster: jettonMasterAddress,
                        comment: depositCode || "(no valid deposit code)",
                    });

                    if (depositCode) {
                        const depositData = {
                            transactionHash: event.eventId,
                            transactionLt: event.lt?.toString() || "0",
                            bodyHash: event.eventId,
                            comment: depositCode,
                            amountNano: amountNano,
                            sender: senderStr,
                            assetType: "JETTON" as const,
                            jettonMasterAddress: jettonMasterAddress,
                            jettonSymbol: jettonSymbol,
                            jettonDecimals: jettonDecimals,
                        };

                        console.log(`[deposit-monitor] ✅ Valid Jetton deposit code found: ${depositCode}`);
                        console.log(`[deposit-monitor] Amount: ${amountReadable} ${jettonSymbol}, Sender: ${senderStr}`);

                        try {
                            await publishDepositTransaction(rabbitChannel, rabbitExchange, depositData);
                            console.log(`[deposit-monitor] 📤 Published Jetton deposit event to backend`);
                        } catch (publishError) {
                            console.error(`[deposit-monitor] ❌ Failed to publish deposit event:`, publishError);
                        }
                    } else {
                        console.log(`[deposit-monitor] Jetton transfer without deposit code (normal transfer)`);
                    }
                }
            }

            newEventCount++;
        }

        // Update lastProcessedLt after processing all events
        if (maxLtSeen > lastProcessedLt) {
            lastProcessedLt = maxLtSeen;
        }

        if (newEventCount > 0) {
            console.log(`[deposit-monitor] Processed ${newEventCount} new event(s), lastProcessedLt=${lastProcessedLt.toString()}`);
        }
    } catch (error) {
        console.error("[deposit-monitor] Error fetching events:", error);
        throw error;
    }
}

// Helper function to validate deposit code format
function validateDepositCode(comment: string): string | null {
    if (!comment) return null;

    const normalized = comment.trim().toUpperCase();

    // Check if it matches the 6-character code format
    const CODE_REGEX = /^[A-Z0-9]{6}$/;
    if (!CODE_REGEX.test(normalized)) {
        return null;
    }

    return normalized;
}

async function publishDepositTransaction(
    channel: Channel,
    exchange: string,
    data: {
        transactionHash: string;
        transactionLt: string;
        bodyHash: string;
        comment: string;
        amountNano: string;
        sender: string;
        assetType: "TON" | "JETTON";
        jettonMasterAddress: string | null;
        jettonSymbol: string | null;
        jettonDecimals: number | null;
    }
) {
    const event = {
        type: "deposit.transaction-found",
        occurredAt: new Date().toISOString(),
        data,
    };

    const routingKey = "deposit.transaction-found";

    const assetDisplay = data.assetType === "JETTON"
        ? `${data.jettonSymbol} (Jetton)`
        : "TON";

    console.log(`[deposit-monitor] Publishing event:`, {
        type: event.type,
        code: data.comment,
        assetType: data.assetType,
        asset: assetDisplay,
        amount: data.amountNano,
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
