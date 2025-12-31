import {Address} from "@ton/core";
import {TonClient} from "@ton/ton";

export type Network = "mainnet" | "testnet";

export function sleep(ms: number) {
    return new Promise((r) => setTimeout(r, ms));
}

export async function waitForSeqnoIncrement(
    provider: { getSeqno: () => Promise<number> },
    prev: number,
    timeoutMs = 90_000,
    pollMs = 1500
): Promise<number> {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
        const now = await provider.getSeqno();
        if (now > prev) return now;
        await sleep(pollMs);
    }
    throw new Error(`Timeout waiting for seqno to increment (stuck at ${prev})`);
}

export const waitSeqno = waitForSeqnoIncrement;

export function bufToHex(b: Uint8Array | Buffer) {
    return Buffer.isBuffer(b) ? b.toString("hex") : Buffer.from(b).toString("hex");
}

export function withTimeout<T>(p: Promise<T>, ms: number, err: Error): Promise<T> {
    return new Promise((resolve, reject) => {
        const t = setTimeout(() => reject(err), ms);
        p.then(
            (v) => {
                clearTimeout(t);
                resolve(v);
            },
            (e) => {
                clearTimeout(t);
                reject(e);
            }
        );
    });
}

export function normalizeStringAddr(s?: string): string {
    if (!s) return "";
    return normalizeAddr(Address.parse(s));
}

export function normalizeAddr(a: Address): string {
    return a.toString({ bounceable: false });
}

export function parseBig(s?: string | null): bigint | null {
    if (!s) return null;
    try {
        return BigInt(s);
    } catch {
        return null;
    }
}

export async function getAccountState(client: TonClient, addr: Address) {
    return client.getContractState(addr);
}
