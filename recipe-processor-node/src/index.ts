import {publishJson, setupRabbit, shutdown, startConsumer} from "./rabbit.js";
import {mockSendTon, sendTon} from "./recipes/transactions.js";
import {startPoolsUpdater} from "./stonfi/poolsCache.js";
import { Address } from "@ton/core";
import { swapTonToToken as doSwapTonToToken, swapTokenToTon as doSwapTokenToTon } from "./recipes/swap.js";
import { handleWalletCreationRequest } from "./recipes/walletCreation.js";
import { startMultiWalletMonitoring, addWalletToMonitor } from "./recipes/multiWalletMonitor.js";


startPoolsUpdater();

const RABBIT_URL = process.env.RABBIT_URL || "amqp://guest:guest@localhost:5672/";
const SERVICE = "recipe-processor-node";

const { conn, ch, exchange, queue } = await setupRabbit(RABBIT_URL, SERVICE, ["agent-llm.#", "wallet.#", "deposit.#"]);

// Start multi-wallet monitoring (session-based)
startMultiWalletMonitoring(ch, exchange).catch((err) => {
    console.error("[recipe-processor-node] Failed to start multi-wallet monitoring:", err);
});

await startConsumer(ch, queue, async (_msg, body) => {
    try {
        if (!body || typeof body !== "object") return;
        const { type, data, occurredAt } = body;

        // Handle wallet events
        if (type === "wallet.create-request") {
            await handleWalletCreationRequest(data, ch, exchange);
            return;
        }

        // Handle deposit session events
        if (type === "deposit.session-started") {
            const userId = data?.userId;
            const walletAddress = data?.walletAddress;
            const expiresAt = data?.expiresAt;

            if (userId && walletAddress && expiresAt) {
                addWalletToMonitor(walletAddress, userId, new Date(expiresAt));
                console.log(`[${SERVICE}] Deposit session started: wallet=${walletAddress}, user=${userId}, expiresAt=${expiresAt}`);
            } else {
                console.error(`[${SERVICE}] Invalid deposit.session-started event:`, data);
            }
            return;
        }

        if (type === "agent-llm.send-ton") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const amount = data?.tonAmount;
            const receiver = data?.receiverAddress;
            console.log(`[${SERVICE}] send-ton requested:`, { messageId, userId, amount, receiver });

            try {
                const txId = await sendTon(amount, receiver);
                console.log(`[${SERVICE}] send-ton done: txId=${txId}`);
                publishJson(ch, exchange, "agent-llm.send-ton.result", {
                    type: "agent-llm.send-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tonAmount: amount,
                        receiverAddress: receiver,
                        success: true,
                        txId,
                    },
                });
            } catch (err: any) {
                console.error(`[${SERVICE}] send-ton error:`, err);
                publishJson(ch, exchange, "agent-llm.send-ton.result", {
                    type: "agent-llm.send-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tonAmount: amount,
                        receiverAddress: receiver,
                        success: false,
                        error: String(err?.message || err),
                    },
                });
            }
        } else if (type === "agent-llm.swap-ton-to-token") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const jettonMaster = data?.jettonMaster;
            const minimalTokenAmount = data?.minimalTokenAmount;
            const swapTonAmount = data?.swapTonAmount;
            const poolAddress = data?.poolAddress as string;
            console.log(`[${SERVICE}] swap-ton-to-token requested:`, { messageId, userId, jettonMaster, minimalTokenAmount, swapTonAmount, poolAddress });


            const swapAmtNum = Number(swapTonAmount);
            if (!Number.isFinite(swapAmtNum) || swapAmtNum <= 0) {
                publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                    type: "agent-llm.swap-ton-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: `Invalid swapTonAmount: ${swapTonAmount}`,
                    },
                });
                return;
            } else {
                console.error("Swap TON amount failed checks, value is " + swapAmtNum)
            }

            try {
                const res = await doSwapTonToToken(
                    Number(userId),
                    Address.parse(jettonMaster),
                    Number(minimalTokenAmount),
                    swapAmtNum,
                    poolAddress,
                );

                if (res.ok) {
                    publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                        type: "agent-llm.swap-ton-to-token.result",
                        occurredAt: new Date().toISOString(),
                        correlation: { occurredAt },
                        data: {
                            messageId,
                            userId,
                            success: true,
                            txId: res.txId,
                            router: res.router,
                            pool: res.pool,
                            pTon: res.pTon,
                            jettonMinter: res.jettonMinter,
                            offerNanotons: res.offerNanotons,
                            minAskNano: res.minAskNano,
                            requestedJettonMaster: jettonMaster,
                            requestedMinimalTokenAmount: minimalTokenAmount,
                            requestedSwapTonAmount: swapAmtNum,
                        },
                    });
                } else {
                    publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                        type: "agent-llm.swap-ton-to-token.result",
                        occurredAt: new Date().toISOString(),
                        correlation: { occurredAt },
                        data: {
                            messageId,
                            userId,
                            success: false,
                            error: res.error,
                            details: res.details,
                            requestedJettonMaster: jettonMaster,
                            requestedMinimalTokenAmount: minimalTokenAmount,
                            requestedSwapTonAmount: swapAmtNum,
                        },
                    });
                }
            } catch (err: any) {
                console.error(`[${SERVICE}] swap-ton-to-token error:`, err);
                publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                    type: "agent-llm.swap-ton-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: String(err?.message || err),
                    },
                });
            }
        } else if (type === "agent-llm.swap-token-to-ton") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const jettonMaster = data?.jettonMaster;
            const minimalTonAmount = data?.minimalTonAmount;
            const swapTokenAmount = data?.swapTokenAmount;
            const poolAddress = data?.poolAddress as string;
            console.log(`[${SERVICE}] swap-token-to-ton requested:`, { messageId, userId, jettonMaster, minimalTonAmount, swapTokenAmount, poolAddress });

            const swapTokenAmtNum = Number(swapTokenAmount);
            if (!Number.isFinite(swapTokenAmtNum) || swapTokenAmtNum <= 0) {
                publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                    type: "agent-llm.swap-token-to-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: `Invalid swapTokenAmount: ${swapTokenAmount}`,
                    },
                });
                return;
            } else {
                console.error("Swap token amount failed checks, value is " + swapTokenAmtNum)
            }

            try {
                const res = await doSwapTokenToTon(
                    Number(userId),
                    Address.parse(jettonMaster),
                    Number(minimalTonAmount),
                    swapTokenAmtNum,
                    poolAddress,
                );

                if (res.ok) {
                    publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                        type: "agent-llm.swap-token-to-ton.result",
                        occurredAt: new Date().toISOString(),
                        correlation: { occurredAt },
                        data: {
                            messageId,
                            userId,
                            success: true,
                            txId: res.txId,
                            router: res.router,
                            pool: res.pool,
                            pTon: res.pTon,
                            jettonMinter: res.jettonMinter,
                            offerNanotons: res.offerNanotons,
                            minAskNano: res.minAskNano,
                            requestedJettonMaster: jettonMaster,
                            requestedMinimalTonAmount: minimalTonAmount,
                            requestedSwapTokenAmount: swapTokenAmtNum,
                        },
                    });
                } else {
                    publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                        type: "agent-llm.swap-token-to-ton.result",
                        occurredAt: new Date().toISOString(),
                        correlation: { occurredAt },
                        data: {
                            messageId,
                            userId,
                            success: false,
                            error: res.error,
                            details: res.details,
                            requestedJettonMaster: jettonMaster,
                            requestedMinimalTonAmount: minimalTonAmount,
                            requestedSwapTokenAmount: swapTokenAmtNum,
                        },
                    });
                }
            } catch (err: any) {
                console.error(`[${SERVICE}] swap-token-to-ton error:`, err);
                publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                    type: "agent-llm.swap-token-to-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: String(err?.message || err),
                    },
                });
            }
        }

    } catch (e) {
        console.error(`[${SERVICE}] error handling message:`, e);
        throw e;
    }
});

// Graceful stop
process.on("SIGINT", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
process.on("SIGTERM", async () => { await shutdown({ conn, ch, exchange, queue }); process.exit(0); });
