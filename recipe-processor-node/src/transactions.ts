import { TonClient, WalletContractV5R1, internal, toNano } from "@ton/ton";
import { mnemonicToPrivateKey } from "@ton/crypto";
import { Address, OpenedContract, SendMode } from "@ton/core";
import { mnemonic_array } from "./mnemonics.js"; // Expected to be provided

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";

function sleep(ms: number) {
    return new Promise((r) => setTimeout(r, ms));
}

export async function waitForSeqnoIncrement(
    provider: OpenedContract<any>,
    prev: number,
    timeoutMs = 90_000,
    pollMs = 1500
) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
        // @ts-ignore
        const now = await provider.getSeqno();
        if (now > prev) return now;
        await sleep(pollMs);
    }
    throw new Error(`Timeout waiting for seqno to increment (stuck at ${prev})`);
}

function bufToHex(b: Uint8Array | Buffer) {
    return Buffer.isBuffer(b) ? b.toString("hex") : Buffer.from(b).toString("hex");
}

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
    console.log("[ton] Current seqno:", before);

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
        console.error("[ton] sendTransfer FAILED:", e?.response?.status, e?.response?.statusText);
        if (e?.response?.data) console.error("[ton] Toncenter body:", JSON.stringify(e.response.data));
        throw e;
    }

    console.log("[ton] Sent. Waiting for inclusion...");
    await waitForSeqnoIncrement(provider, before, 90_000, 1500);
    console.log("[ton] Transfer confirmed");

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
    console.log("[ton] tx id:", txId);
    return txId;
}
