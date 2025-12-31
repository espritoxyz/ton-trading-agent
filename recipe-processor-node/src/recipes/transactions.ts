import {internal, toNano, TonClient, WalletContractV5R1} from "@ton/ton";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {Address, SendMode} from "@ton/core";
import {mnemonic_array} from "../mnemonics.js"; // Expected to be provided
import {randomBytes} from "crypto";
import {bufToHex, sleep, waitForSeqnoIncrement} from "../utils.js";

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";

// utils imported from ./utils.js

/**
 * Sends specified TON amount to a given recipient address.
 * @param amountTon amount in TON (number or string), e.g. 0.3
 * @param receiverAddress TON address (raw or user-friendly)
 * @returns txId (hex) when located in recent transactions
 */
export async function sendTon(amountTon: number | string, receiverAddress: string): Promise<string> {
    if (!receiverAddress) throw new Error("receiverAddress is required");
    const amountStr = String(amountTon);

    const client = new TonClient({ endpoint, apiKey });
    const { publicKey, secretKey } = await mnemonicToPrivateKey(mnemonic_array);

    const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
    const provider = client.open(wallet);

    const before: number = await provider.getSeqno();
    console.log("[MOCK ton] Current seqno:", before);

    const recipient = Address.parse(receiverAddress);
    const amount = toNano(amountStr);

    const transfer = internal({
        to: recipient,
        value: amount,
        bounce: false,
        body: undefined,
    });

    try {
        await provider.sendTransfer({
            seqno: before,
            secretKey: secretKey,
            messages: [transfer],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });
    } catch (e: any) {
        console.error("[MOCK ton] sendTransfer FAILED:", e?.response?.status, e?.response?.statusText);
        if (e?.response?.data) console.error("[ton] Toncenter body:", JSON.stringify(e.response.data));
        throw e;
    }

    console.log("[MOCK ton] Sent. Waiting for inclusion...");
    await waitForSeqnoIncrement(provider, before, 90_000, 1500);
    console.log("[MOCK ton] Transfer confirmed");

    const txs = await client.getTransactions(wallet.address, { limit: 10 });

    const tx = txs.find((t) => {
        const outs =
            typeof t.outMessages.values === "function"
                ? Array.from(t.outMessages.values())
                : Object.values(t.outMessages as any);

        return (
            t.inMessage?.info.type === "external-in" &&
            outs.some((m: any) =>
                m.info?.type === "internal" &&
                (m.info.dest?.equals?.(recipient) ?? m.info.dest?.toString() === recipient.toString()) &&
                m.info.value?.coins >= amount
            )
        );
    });

    if (!tx) throw new Error("Transaction not found");

    const rawHash = tx.hash();
    const txId = bufToHex(rawHash);
    console.log("[MOCK ton] tx id:", txId);
    return txId;
}

/**
 * Mocked version of sendTon that does not touch the blockchain but mimics timings and returns a fake tx id.
 */
export async function mockSendTon(amountTon: number | string, receiverAddress: string): Promise<string> {
    if (!receiverAddress) throw new Error("receiverAddress is required");
    const amountStr = String(amountTon);

    // Simulate pre-send state
    const before = Math.floor(Math.random() * 10000);
    console.log("[ton-mock] Current seqno:", before);

    // Pretend to send and wait for inclusion
    console.log(`{\"type\":\"agent-llm.send-ton\",\"note\":\"mock sending\",\"amount\":${amountStr},\"to\":\"${receiverAddress}\"}`);
    console.log("[ton-mock] Sent. Waiting for inclusion...");
    await sleep(300 + Math.floor(Math.random() * 500));
    console.log("[ton-mock] Transfer confirmed");

    // Generate a fake 32-byte tx id (hex)
    const rawHash = randomBytes(32);
    const txId = bufToHex(rawHash);
    console.log("[ton-mock] tx id:", txId);
    return txId;
}
