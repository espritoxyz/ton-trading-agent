import { TonApiClient } from '@ton-api/client';
import { Address } from "@ton/core";
import type { Channel } from "amqplib";

const TONAPI_BASE_URL = process.env.TONAPI_BASE_URL;
const TONAPI_KEY = process.env.TONAPI_KEY;

export interface WalletBalance {
    userId: number;
    walletAddress: string;
    tonBalance: string; // in nanotons
    jettons: JettonBalance[];
}

export interface JettonBalance {
    jettonMasterAddress: string;
    balance: string; // in smallest units (nano)
    symbol?: string;
    decimals?: number;
    name?: string;
}

/**
 * Fetch complete wallet state (TON balance + all jettons)
 */
export async function fetchWalletBalance(walletAddress: string, userId: number): Promise<WalletBalance> {
    const client = new TonApiClient({
        baseUrl: TONAPI_BASE_URL,
        apiKey: TONAPI_KEY,
    });

    const address = Address.parse(walletAddress);

    // Fetch account info (for TON balance)
    const account = await client.accounts.getAccount(address);
    const tonBalance = account.balance.toString();

    // Fetch jetton balances
    const jettonsData = await client.accounts.getAccountJettonsBalances(address);

    const jettons: JettonBalance[] = [];
    for (const jetton of jettonsData.balances) {
        jettons.push({
            jettonMasterAddress: jetton.jetton.address.toString(),
            balance: jetton.balance.toString(),
            symbol: jetton.jetton.symbol,
            decimals: jetton.jetton.decimals,
            name: jetton.jetton.name,
        });
    }

    console.log(`[wallet-balance-sync] Fetched balance for ${walletAddress}: TON=${tonBalance}, Jettons=${jettons.length}`);

    return {
        userId,
        walletAddress,
        tonBalance,
        jettons,
    };
}

/**
 * Sync wallet balance and publish to RabbitMQ
 */
export async function syncWalletBalance(
    walletAddress: string,
    userId: number,
    channel: Channel,
    exchange: string
): Promise<void> {
    try {
        const balance = await fetchWalletBalance(walletAddress, userId);

        // Publish balance sync event
        const message = {
            type: "wallet.balance-synced",
            occurredAt: new Date().toISOString(),
            data: {
                userId: balance.userId,
                walletAddress: balance.walletAddress,
                tonBalance: balance.tonBalance,
                jettons: balance.jettons,
            }
        };

        channel.publish(
            exchange,
            "wallet.balance-synced",
            Buffer.from(JSON.stringify(message)),
            { persistent: true }
        );

        console.log(`[wallet-balance-sync] Published balance sync event for user ${userId}`);
    } catch (error: any) {
        console.error(`[wallet-balance-sync] Error syncing balance for ${walletAddress}:`, error?.message || error);
        throw error;
    }
}
