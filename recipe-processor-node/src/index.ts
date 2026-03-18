import {publishJson, setupRabbit, shutdown, startConsumer} from "./rabbit.js";
import {mockSendTon, sendTon, sendToken} from "./recipes/transactions.js";
import { Address } from "@ton/core";
import { swapTonToToken as doSwapTonToToken, swapTokenToTon as doSwapTokenToTon, swapTokenToToken as doSwapTokenToToken } from "./recipes/swap.js";

import { handleWalletCreationRequest } from "./recipes/walletCreation.js";
import { startMultiWalletMonitoring, addWalletToMonitor } from "./recipes/multiWalletMonitor.js";
import { syncWalletBalance } from "./recipes/walletBalanceSync.js";


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
            const walletAddress = data?.walletAddress;
            const mnemonic = data?.mnemonic as string[] | undefined;
            console.log(`[${SERVICE}] send-ton requested:`, { messageId, userId, amount, receiver });

            if (!mnemonic || !Array.isArray(mnemonic) || mnemonic.length === 0) {
                console.error(`[${SERVICE}] send-ton error: missing or invalid mnemonic for user ${userId}`);
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
                        error: "User has no wallet or mnemonic not provided",
                    },
                });
                return;
            }

            try {
                const result = await sendTon(amount, receiver, mnemonic);

                const txId = result.ok ? result.txId : result.txId;
                const exitCode = result.ok ? undefined : result.exitCode;
                const error = result.ok ? undefined : result.error;
                const success = result.ok && !error;
                const totalFee = result.totalFee;

                if (success && walletAddress && userId) {
                    try {
                        await syncWalletBalance(walletAddress, userId, ch, exchange);
                    } catch (syncErr: any) {
                        console.error(`[${SERVICE}] Failed to sync wallet balance after send-ton:`, syncErr?.message);
                        // Don't fail the operation if sync fails
                    }
                }

                if (success && txId) {
                    console.log(`[${SERVICE}] send-ton done: txId=${txId}`);
                } else if (error) {
                    console.error(`[${SERVICE}] send-ton failed at TVM/action level`, { txId, exitCode, error });
                }

                publishJson(ch, exchange, "agent-llm.send-ton.result", {
                    type: "agent-llm.send-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tonAmount: amount,
                        receiverAddress: receiver,
                        success,
                        txId,
                        totalFee,
                        exitCode,
                        error,
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
        } else if (type === "agent-llm.send-token") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const amountHuman = data?.tokenAmount;
            const amountNano = data?.tokenAmountNano;
            const jettonMaster = data?.jettonMaster;
            const receiver = data?.receiverAddress;
            const walletAddress = data?.walletAddress;
            const mnemonic = data?.mnemonic as string[] | undefined;
            console.log(`[${SERVICE}] send-token requested:`, { messageId, userId, amountHuman, amountNano, jettonMaster, receiver });

            if (!mnemonic || !Array.isArray(mnemonic) || mnemonic.length === 0) {
                console.error(`[${SERVICE}] send-token error: missing or invalid mnemonic for user ${userId}`);
                publishJson(ch, exchange, "agent-llm.send-token.result", {
                    type: "agent-llm.send-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tokenAmount: amountHuman,
                        tokenAmountNano: amountNano,
                        jettonMaster,
                        receiverAddress: receiver,
                        success: false,
                        error: "User has no wallet or mnemonic not provided",
                    },
                });
                return;
            }

            try {
                const result = await sendToken(jettonMaster, amountNano, receiver, mnemonic);

                const txId = result.ok ? result.txId : result.txId;
                const exitCode = result.ok ? undefined : result.exitCode;
                const error = result.ok ? undefined : result.error;
                const success = result.ok && !error;
                const totalFee = result.totalFee;

                if (success && walletAddress && userId) {
                    try {
                        await syncWalletBalance(walletAddress, userId, ch, exchange);
                    } catch (syncErr: any) {
                        console.error(`[${SERVICE}] Failed to sync wallet balance after send-token:`, syncErr?.message);
                        // Don't fail the operation if sync fails
                    }
                }

                if (success && txId) {
                    console.log(`[${SERVICE}] send-token done: txId=${txId}`);
                } else if (error) {
                    console.error(`[${SERVICE}] send-token failed at TVM/action level`, { txId, exitCode, error });
                }

                publishJson(ch, exchange, "agent-llm.send-token.result", {
                    type: "agent-llm.send-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tokenAmount: amountHuman,
                        tokenAmountNano: amountNano,
                        jettonMaster,
                        receiverAddress: receiver,
                        success,
                        txId,
                        totalFee,
                        exitCode,
                        error,
                    },
                });
            } catch (err: any) {
                console.error(`[${SERVICE}] send-token error:`, err);
                publishJson(ch, exchange, "agent-llm.send-token.result", {
                    type: "agent-llm.send-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        tokenAmount: amountHuman,
                        tokenAmountNano: amountNano,
                        jettonMaster,
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
            const walletAddress = data?.walletAddress;
            const mnemonic = data?.mnemonic as string[] | undefined;
            console.log(`[${SERVICE}] swap-ton-to-token requested:`, { messageId, userId, jettonMaster, minimalTokenAmount, swapTonAmount, poolAddress });

            if (!mnemonic || !Array.isArray(mnemonic) || mnemonic.length === 0) {
                console.error(`[${SERVICE}] swap-ton-to-token error: missing or invalid mnemonic for user ${userId}`);
                publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                    type: "agent-llm.swap-ton-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: "User has no wallet or mnemonic not provided",
                    },
                });
                return;
            }

            const swapAmtNum = Number(swapTonAmount);
            if (!Number.isFinite(swapAmtNum) || swapAmtNum <= 0) {
                console.error(`[${SERVICE}] swap-ton-to-token error: invalid swapTonAmount`, {
                    swapTonAmount,
                    parsed: swapAmtNum,
                });
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
            }


            try {
                const res = await doSwapTonToToken(
                    Address.parse(jettonMaster),
                    Number(minimalTokenAmount),
                    swapAmtNum,
                    poolAddress,
                    mnemonic,
                );

                const txId = res.ok ? res.txId : undefined;
                const error = res.ok ? undefined : res.error;
                const success = res.ok && !error;
                const totalFee = res.totalFee;

                if (success && walletAddress && userId) {
                    try {
                        await syncWalletBalance(walletAddress, userId, ch, exchange);
                    } catch (syncErr: any) {
                        console.error(`[${SERVICE}] Failed to sync wallet balance after swap-ton-to-token:`, syncErr?.message);
                        // Don't fail the operation if sync fails
                    }
                }

                publishJson(ch, exchange, "agent-llm.swap-ton-to-token.result", {
                    type: "agent-llm.swap-ton-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success,
                        txId,
                        totalFee,
                        error,
                        router: success ? res.router : undefined,
                        pool: success ? res.pool : undefined,
                        pTon: success ? res.pTon : undefined,
                        jettonMinter: success ? res.jettonMinter : undefined,
                        offerNanotons: success ? res.offerNanotons : undefined,
                        minAskNano: success ? res.minAskNano : undefined,
                        askNano: success ? res.askNano : undefined,
                        requestedJettonMaster: jettonMaster,
                        requestedMinimalTokenAmount: minimalTokenAmount,
                        requestedSwapTonAmount: swapAmtNum,
                    },
                });

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
        } else if (type === "agent-llm.swap-token-to-token") {
            const messageId = data?.messageId;
            const userId = data?.userId;
            const offerJettonMaster = data?.offerJettonMaster;
            const askJettonMaster = data?.askJettonMaster;
            const minimalAskTokenAmount = data?.minimalAskTokenAmount;
            const swapOfferTokenAmount = data?.swapOfferTokenAmount;
            const poolAddress = data?.poolAddress as string;
            const walletAddress = data?.walletAddress;
            const mnemonic = data?.mnemonic as string[] | undefined;
            console.log(`[${SERVICE}] swap-token-to-token requested:`, { messageId, userId, offerJettonMaster, askJettonMaster, minimalAskTokenAmount, swapOfferTokenAmount, poolAddress });

            if (!mnemonic || !Array.isArray(mnemonic) || mnemonic.length === 0) {
                console.error(`[${SERVICE}] swap-token-to-token error: missing or invalid mnemonic for user ${userId}`);
                publishJson(ch, exchange, "agent-llm.swap-token-to-token.result", {
                    type: "agent-llm.swap-token-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: "User has no wallet or mnemonic not provided",
                    },
                });
                return;
            }

            const swapOfferTokenAmtNum = Number(swapOfferTokenAmount);
            if (!Number.isFinite(swapOfferTokenAmtNum) || swapOfferTokenAmtNum <= 0) {
                console.error(`[${SERVICE}] swap-token-to-token error: invalid swapOfferTokenAmount`, {
                    swapOfferTokenAmount,
                    parsed: swapOfferTokenAmtNum,
                });
                publishJson(ch, exchange, "agent-llm.swap-token-to-token.result", {
                    type: "agent-llm.swap-token-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: `Invalid swapOfferTokenAmount: ${swapOfferTokenAmount}`,
                    },
                });
                return;
            }

            try {
                const res = await doSwapTokenToToken(
                    Address.parse(offerJettonMaster),
                    Address.parse(askJettonMaster),
                    Number(minimalAskTokenAmount),
                    swapOfferTokenAmtNum,
                    poolAddress,
                    mnemonic,
                );

                const txId = res.ok ? res.txId : undefined;
                const error = res.ok ? undefined : res.error;
                const success = res.ok && !error;
                const totalFee = res.totalFee;

                if (success && walletAddress && userId) {
                    try {
                        await syncWalletBalance(walletAddress, userId, ch, exchange);
                    } catch (syncErr: any) {
                        console.error(`[${SERVICE}] Failed to sync wallet balance after swap-token-to-token:`, syncErr?.message);
                    }
                }

                publishJson(ch, exchange, "agent-llm.swap-token-to-token.result", {
                    type: "agent-llm.swap-token-to-token.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success,
                        txId,
                        totalFee,
                        error,
                        router: success ? res.router : undefined,
                        pool: success ? res.pool : undefined,
                        pTon: success ? res.pTon : undefined,
                        jettonMinter: success ? res.jettonMinter : undefined,
                        offerNanotons: success ? res.offerNanotons : undefined,
                        minAskNano: success ? res.minAskNano : undefined,
                        askNano: success ? res.askNano : undefined,
                        requestedOfferJettonMaster: offerJettonMaster,
                        requestedAskJettonMaster: askJettonMaster,
                        requestedMinimalAskTokenAmount: minimalAskTokenAmount,
                        requestedSwapOfferTokenAmount: swapOfferTokenAmtNum,
                    },
                });

            } catch (err: any) {

                console.error(`[${SERVICE}] swap-token-to-token error:`, err);
                publishJson(ch, exchange, "agent-llm.swap-token-to-token.result", {
                    type: "agent-llm.swap-token-to-token.result",
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
            const walletAddress = data?.walletAddress;
            const mnemonic = data?.mnemonic as string[] | undefined;
            console.log(`[${SERVICE}] swap-token-to-ton requested:`, { messageId, userId, jettonMaster, minimalTonAmount, swapTokenAmount, poolAddress });

            if (!mnemonic || !Array.isArray(mnemonic) || mnemonic.length === 0) {
                console.error(`[${SERVICE}] swap-token-to-ton error: missing or invalid mnemonic for user ${userId}`);
                publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                    type: "agent-llm.swap-token-to-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success: false,
                        error: "User has no wallet or mnemonic not provided",
                    },
                });
                return;
            }

            const swapTokenAmtNum = Number(swapTokenAmount);
            if (!Number.isFinite(swapTokenAmtNum) || swapTokenAmtNum <= 0) {
                console.error(`[${SERVICE}] swap-token-to-ton error: invalid swapTokenAmount`, {
                    swapTokenAmount,
                    parsed: swapTokenAmtNum,
                });
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
            }


            try {
                const res = await doSwapTokenToTon(
                    Number(userId),
                    Address.parse(jettonMaster),
                    Number(minimalTonAmount),
                    swapTokenAmtNum,
                    poolAddress,
                    mnemonic,
                );

                const txId = res.ok ? res.txId : undefined;
                const error = res.ok ? undefined : res.error;
                const success = res.ok && !error;
                const totalFee = res.totalFee;

                if (success && walletAddress && userId) {
                    try {
                        await syncWalletBalance(walletAddress, userId, ch, exchange);
                    } catch (syncErr: any) {
                        console.error(`[${SERVICE}] Failed to sync wallet balance after swap-token-to-ton:`, syncErr?.message);
                        // Don't fail the operation if sync fails
                    }
                }

                publishJson(ch, exchange, "agent-llm.swap-token-to-ton.result", {
                    type: "agent-llm.swap-token-to-ton.result",
                    occurredAt: new Date().toISOString(),
                    correlation: { occurredAt },
                    data: {
                        messageId,
                        userId,
                        success,
                        txId,
                        totalFee,
                        error,
                        router: success ? res.router : undefined,
                        pool: success ? res.pool : undefined,
                        pTon: success ? res.pTon : undefined,
                        jettonMinter: success ? res.jettonMinter : undefined,
                        offerNanotons: success ? res.offerNanotons : undefined,
                        minAskNano: success ? res.minAskNano : undefined,
                        askNano: success ? res.askNano : undefined,
                        requestedJettonMaster: jettonMaster,
                        requestedMinimalTonAmount: minimalTonAmount,
                        requestedSwapTokenAmount: swapTokenAmtNum,
                    },
                });

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
