import {internal, toNano, TonClient, WalletContractV5R1} from "@ton/ton";
import https from "https";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {Address, SendMode, beginCell, Cell, toNano as coreToNano} from "@ton/core";
import {mnemonic_array} from "../mnemonics.js"; // Expected to be provided
import {randomBytes} from "crypto";
import {bufToHex, sleep, waitForSeqnoIncrement} from "../utils.js";

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";

export async function sendTon(amountTon: number | string, receiverAddress: string): Promise<string> {
    if (!receiverAddress) throw new Error("receiverAddress is required");
    const amountStr = String(amountTon);

    const client = new TonClient({ endpoint, apiKey });
    const { publicKey, secretKey } = await mnemonicToPrivateKey(mnemonic_array);

    const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
    const provider = client.open(wallet);

    const before: number = await provider.getSeqno();
    console.log("Current seqno:", before);

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
        console.error("sendTransfer FAILED:", e?.response?.status, e?.response?.statusText);
        if (e?.response?.data) console.error("Toncenter body:", JSON.stringify(e.response.data));
        throw e;
    }

    console.log("Sent. Waiting for inclusion...");
    await waitForSeqnoIncrement(provider, before, 90_000, 1500);
    console.log("Transfer confirmed");

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
    console.log("tx id:", txId);
    return txId;
}

export async function sendToken(
    jettonMasterAddress: string,
    amountJetton: string | number,
    receiverAddress: string,
    forwardTonAmount: number | string = 0.0,
): Promise<string> {
    if (!jettonMasterAddress) throw new Error("jettonMasterAddress is required");
    if (!receiverAddress) throw new Error("receiverAddress is required");

    const client = new TonClient({ endpoint, apiKey });

    const { publicKey, secretKey } = await mnemonicToPrivateKey(mnemonic_array);

    const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
    const provider = client.open(wallet);

    const before: number = await provider.getSeqno();
    console.log("Current seqno (jetton):", before);

    const jettonMaster = Address.parse(jettonMasterAddress);
    const owner = wallet.address;

    const res = await client.runMethod(jettonMaster, "get_wallet_address", [
        { type: "slice", cell: beginCell().storeAddress(owner).endCell() },
    ]);
    const addrCell = res.stack.readCell();
    const slice = addrCell.beginParse();
    const jettonWallet = slice.loadAddress();

    const recipient = Address.parse(receiverAddress);

    const jettonAmountBigInt = BigInt(amountJetton.toString());
    const forwardAmount = coreToNano(String(forwardTonAmount));

    // Jetton transfer op code (32 bits) as per standard jetton specification.
    const OP_JETTON_TRANSFER = 0xf8a7ea5;

    const body: Cell = beginCell()
        .storeUint(OP_JETTON_TRANSFER, 32) // op
        .storeUint(0, 64) // query_id
        .storeCoins(jettonAmountBigInt) // jetton amount (in smallest units)
        .storeAddress(recipient) // to address
        .storeAddress(wallet.address) // response_destination (sender wallet)
        .storeBit(false) // no custom payload
        .storeCoins(forwardAmount) // forward_ton_amount
        .storeBit(false) // no forward payload
        .endCell();

    // Use Toncenter v3 estimateFee API to determine the value to send to jetton wallet for gas.
    const { valueForGas } = await estimateJettonTransferFee(jettonWallet, body);

    const transfer = internal({
        to: jettonWallet,
        value: valueForGas,

        bounce: true,
        body,
    });

    try {
        await provider.sendTransfer({
            seqno: before,
            secretKey: secretKey,
            messages: [transfer],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });
    } catch (e: any) {
        console.error("sendToken FAILED:", e?.response?.status, e?.response?.statusText ?? e?.message);
        throw e;
    }


    console.log("Jetton transfer sent. Waiting for inclusion...");
    await waitForSeqnoIncrement(provider, before, 90_000, 1500);
    console.log("Jetton transfer confirmed");

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
                (m.info.dest?.equals?.(jettonWallet) ?? m.info.dest?.toString() === jettonWallet.toString())
            )
        );
    });

    if (!tx) throw new Error("Jetton transaction not found");

    const rawHash = tx.hash();
    const txId = bufToHex(rawHash);
    console.log("jetton tx id:", txId);
    return txId;
}

async function estimateJettonTransferFee(jettonWallet: Address, body: Cell): Promise<{ valueForGas: bigint }> {
    const fallback = coreToNano("0.05"); // conservative default when estimation fails

    try {
        const url = new URL(endpoint.replace("/jsonRPC", ""));
        url.pathname = "/v2/estimateFee";

        const payload: any = {
            address: jettonWallet.toString({ bounceable: true }),
            body: body.toBoc().toString("base64"),
        };

        if (apiKey) {
            payload.api_key = apiKey;
        }

        const data = await httpPostJson(url, payload);

        const fees = data?.result?.source_fees ?? data?.fees ?? data?.result;
        if (!fees) {
            console.warn("Unexpected estimateFee response format", data);
            return { valueForGas: fallback };
        }

        const totalNanotons =
            BigInt(fees.in_fwd_fee ?? 0) +
            BigInt(fees.storage_fee ?? 0) +
            BigInt(fees.gas_fee ?? 0) +
            BigInt(fees.fwd_fee ?? 0);

        // Add a small safety margin (e.g. +10%)
        const safety = (totalNanotons * 10n) / 100n;
        const valueForGas = totalNanotons + safety;

        return { valueForGas };
    } catch (e) {
        console.warn("estimateJettonTransferFee failed, falling back to default", e);
        return { valueForGas: fallback };
    }
}

async function httpPostJson(url: URL, body: any): Promise<any> {
    return new Promise((resolve, reject) => {
        const data = JSON.stringify(body);

        const options: https.RequestOptions = {
            method: "POST",
            hostname: url.hostname,
            path: url.pathname + (url.search || ""),
            port: url.port || (url.protocol === "https:" ? 443 : 80),
            headers: {
                "Content-Type": "application/json",
                "Content-Length": Buffer.byteLength(data),
            },
        };

        const req = https.request(options, (res) => {
            let raw = "";
            res.setEncoding("utf8");
            res.on("data", (chunk) => (raw += chunk));
            res.on("end", () => {
                // Some environments may return HTML error pages; log and throw a parse error.
                try {
                    const parsed = JSON.parse(raw);
                    resolve(parsed);
                } catch (err) {
                    console.warn("Failed to parse estimateFee response as JSON, body starts with:", raw.slice(0, 200));
                    reject(err);
                }
            });
        });

        req.on("error", (err) => reject(err));
        req.write(data);
        req.end();
    });
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
