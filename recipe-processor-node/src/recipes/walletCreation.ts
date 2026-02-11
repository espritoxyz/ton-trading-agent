import { mnemonicNew } from "@ton/crypto";
import { WalletContractV5R1 } from "@ton/ton";
import type { Channel } from "amqplib";
import { publishJson } from "../rabbit.js";

interface WalletCreationData {
    userId: number;
    workchain: number;
    walletVersion: string;
}

interface CreatedWallet {
    userId: number;
    walletAddress: string;
    mnemonicPhrase: string;
    workchain: number;
    walletVersion: string;
}

/**
 * Generate a new wallet with mnemonic
 */
export async function createWallet(
    userId: number,
    workchain: number = 0,
    walletVersion: string = "V5R1"
): Promise<CreatedWallet> {
    console.log(`[wallet-creation] Creating wallet for user ${userId}`);

    // Generate 24-word mnemonic
    const mnemonic = await mnemonicNew(24);
    const mnemonicPhrase = mnemonic.join(" ");

    // Create wallet contract
    const keyPair = await mnemonicToPrivateKey(mnemonic);
    const wallet = WalletContractV5R1.create({
        workchain,
        publicKey: keyPair.publicKey
    });

    const walletAddress = wallet.address.toString({
        bounceable: false,
        testOnly: false
    });

    console.log(`[wallet-creation] Wallet created for user ${userId}: ${walletAddress}`);

    return {
        userId,
        walletAddress,
        mnemonicPhrase,
        workchain,
        walletVersion
    };
}

/**
 * Convert mnemonic to keypair (needed for wallet creation)
 */
async function mnemonicToPrivateKey(mnemonic: string[]): Promise<{ publicKey: Buffer; secretKey: Buffer }> {
    // @ton/crypto provides this functionality
    const { keyPairFromSeed } = await import("@ton/crypto");
    const { mnemonicToPrivateKey: toKey } = await import("@ton/crypto");

    return await toKey(mnemonic);
}

/**
 * Handle wallet creation request from backend
 */
export async function handleWalletCreationRequest(
    data: WalletCreationData,
    channel: Channel,
    exchange: string
): Promise<void> {
    try {
        const { userId, workchain, walletVersion } = data;

        console.log(`[wallet-creation] Received creation request for user ${userId}`);

        const wallet = await createWallet(userId, workchain, walletVersion);

        // Publish response back to backend
        const response = {
            type: "wallet.create-response",
            occurredAt: new Date().toISOString(),
            data: {
                userId: wallet.userId,
                walletAddress: wallet.walletAddress,
                mnemonicPhrase: wallet.mnemonicPhrase,
                workchain: wallet.workchain,
                walletVersion: wallet.walletVersion
            }
        };

        publishJson(channel, exchange, "wallet.create-response", response);

        console.log(`[wallet-creation] Published response for user ${userId}`);
    } catch (error) {
        console.error(`[wallet-creation] Error creating wallet:`, error);

        // Publish error response
        const errorResponse = {
            type: "wallet.create-error",
            occurredAt: new Date().toISOString(),
            data: {
                userId: data.userId,
                error: error instanceof Error ? error.message : "Unknown error"
            }
        };

        publishJson(channel, exchange, "wallet.create-error", errorResponse);
    }
}
