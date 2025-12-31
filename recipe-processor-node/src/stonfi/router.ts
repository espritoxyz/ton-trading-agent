import {Network, normalizeAddr, normalizeStringAddr, parseBig, withTimeout} from "../utils.js";
import {RouterListResponse, StonRouter} from "./StonfiRouter.js";
import {Address} from "@ton/core";
import {StonPool} from "./pool.js";
import {DEX} from "@ston-fi/sdk";

export interface FetchRoutersOptions {
    network?: Network;             // default: 'mainnet'
    baseUrl?: string;              // default: 'https://api.ston.fi'
    timeoutMs?: number;            // default: 12_000
    retries?: number;              // default: 2 (total attempts = retries+1)
}


export type ChosenPool = {
    pool: Address;
    router: Address;
    pTon: Address;
    token0: Address;
    token1: Address;
    reserve0: bigint;
    reserve1: bigint;
};

export async function fetchStonRouters(opts: FetchRoutersOptions = {}): Promise<StonRouter[]> {
    const {
        network = "mainnet",
        baseUrl = "https://api.ston.fi",
        timeoutMs = 12_000,
        retries = 2,
    } = opts;

    const url = new URL("/v1/routers", baseUrl);
    url.searchParams.set("network", network);

    let lastErr: unknown;
    for (let attempt = 0; attempt <= retries; attempt++) {
        try {
            const res = await withTimeout(
                fetch(url.toString(), {
                    method: "GET",
                    headers: {
                        "accept": "application/json",
                    },
                }),
                timeoutMs,
                new Error(`STON.fi /v1/routers timeout after ${timeoutMs}ms`)
            );

            if (!res.ok) {
                const text = await res.text().catch(() => "");
                throw new Error(`STON.fi /v1/routers HTTP ${res.status} ${res.statusText} — ${text}`);
            }

            const data = (await res.json()) as RouterListResponse;

            if (!data || !Array.isArray(data.router_list)) {
                throw new Error("Unexpected STON.fi response shape: no router_list");
            }

            // Optional: basic shape check
            for (const r of data.router_list) {
                if (typeof r.address !== "string" || typeof r.pton_wallet_address !== "string") {
                    throw new Error("Malformed router item in response");
                }
            }

            return data.router_list;
        } catch (e) {
            lastErr = e;
            if (attempt < retries) {
                await new Promise(r => setTimeout(r, 300 * (attempt + 1)));
                continue;
            }
        }
    }
    throw lastErr instanceof Error ? lastErr : new Error(String(lastErr));
}

function pTonFromRouterAddress(routers: StonRouter[], router: string): Address {
    for (const r of routers) {
        if (normalizeStringAddr(r.address) == normalizeStringAddr(router)) return Address.parse(r.pton_master_address);
    }

    console.error(`No pTon for router ${router} found in all routers`)
    return DEX.v2_1.pTON.address
}

export function pickBestStonfiPoolForPair(
    pools: StonPool[],
    routers: StonRouter[],
    tokenAMinter: Address,
    tokenBMinter?: Address,
    offerAmountInA?: bigint
): ChosenPool | undefined {
    tokenBMinter ??= Address.parse("EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c")
    const aStr = normalizeAddr(tokenAMinter);
    const bStr = normalizeAddr(tokenBMinter);

    let best: ChosenPool | undefined;
    let bestScore: bigint = -1n;

    for (const p of pools) {
        if (p.deprecated) continue;

        const t0 = normalizeStringAddr(p.token0_address);
        const t1 = normalizeStringAddr(p.token1_address);
        let orientation: 0 | 1 | null = null;

        if (t0 === aStr && t1 === bStr) {
            orientation = 0;
        } else if (t0 === bStr && t1 === aStr) {
            orientation = 1;
        }
        else continue;

        const r0 = parseBig(p.token0_balance ?? p.reserve0);
        const r1 = parseBig(p.token1_balance ?? p.reserve1);
        if (r0 === null || r1 === null) continue;
        if (r0 === 0n || r1 === 0n) continue;


        const reserveA = orientation === 0 ? r0 : r1;
        const reserveB = orientation === 0 ? r1 : r0;
        const token0Addr = orientation === 0 ? t0 : t1;
        const token1Addr = orientation === 0 ? t1 : t0;

        let score: bigint;
        if (offerAmountInA && offerAmountInA > 0n) {
            score = estimateCpOutNoFee(reserveA, reserveB, offerAmountInA);
            if (score <= 0n) continue;
        } else {
            score = gmScore(reserveA, reserveB);
        }

        if (score > bestScore) {
            bestScore = score;
            best = {
                pool: Address.parse(p.address),
                router: Address.parse(p.router_address),
                pTon: pTonFromRouterAddress(routers, p.router_address),
                token0: Address.parse(token0Addr),
                token1: Address.parse(token1Addr),
                reserve0: orientation === 0 ? r0 : r1,
                reserve1: orientation === 0 ? r1 : r0,
            };
        }
    }

    return best;
}

function gmScore(a: bigint, b: bigint): bigint {
    const prod = a * b;
    if (prod <= 0n) return 0n;
    // integer sqrt
    let x0 = prod, x1 = (prod >> 1n) + 1n;
    while (x1 < x0) { x0 = x1; x1 = (x1 + prod / x1) >> 1n; }
    return x0;
}

function estimateCpOutNoFee(reserveA: bigint, reserveB: bigint, amountInA: bigint): bigint {
    if (amountInA <= 0n || reserveA <= 0n || reserveB <= 0n) return 0n;
    return (reserveB * amountInA) / (reserveA + amountInA);
}
