import {existsSync, readFileSync, renameSync, writeFileSync} from "node:fs";
import {tmpdir} from "node:os";
import {join} from "node:path";
import {fetchStonPools} from "./pool.js";

export type StonfiPools = any;

const POOLS_FILE = process.env.STONFI_POOLS_FILE || "stonfi-pools.json";
const UPDATE_INTERVAL_MS = Number(process.env.STONFI_POOLS_REFRESH_MS || 30_000);
const NETWORK = (process.env.STONFI_NETWORK || "mainnet") as "mainnet" | "testnet";

let updaterStarted = false;
let lastWriteTs = 0;

function atomicWriteJson(path: string, data: any) {
    const tmp = join(tmpdir(), `stonfi-pools-${Date.now()}.json`);
    writeFileSync(tmp, JSON.stringify(data));
    renameSync(tmp, path);
}

async function updateOnce() {
    try {
        const pools = await fetchStonPools({ network: NETWORK });
        atomicWriteJson(POOLS_FILE, pools);
        lastWriteTs = Date.now();
        if (process.env.DEBUG) console.log(`[stonfi] pools updated (${pools?.length ?? "?"}) -> ${POOLS_FILE}`);
    } catch (e) {
        console.error("[stonfi] pools update failed:", (e as any)?.response?.data ?? (e as any)?.message ?? e);
    }
}

export function startPoolsUpdater() {
    if (updaterStarted) return;
    updaterStarted = true;
    void updateOnce();
    setInterval(() => void updateOnce(), UPDATE_INTERVAL_MS).unref?.();
}

export function readPoolsFromFile(): StonfiPools | null {
    if (!existsSync(POOLS_FILE)) return null;
    try {
        const raw = readFileSync(POOLS_FILE, "utf-8");
        return JSON.parse(raw);
    } catch (e) {
        console.error("[stonfi] failed to read pools file:", e);
        return null;
    }
}

export function getPoolsInfo() {
    return { file: POOLS_FILE, lastWriteTs };
}
