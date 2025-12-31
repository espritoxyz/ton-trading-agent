import {TonClient} from "@ton/ton";
import {Address, beginCell} from "@ton/core";
import {Network, withTimeout, getAccountState} from "../utils.js";


export type StonPool = {
    address: string;
    router_address: string;
    token0_address: string;
    token1_address: string;
    token0_balance?: string;
    token1_balance?: string;
    reserve0?: string;
    reserve1?: string;
    deprecated?: boolean;
};

export type PoolListResponse = {
    pool_list: StonPool[];
};

export interface FetchPoolsOptions {
    network?: Network;             // default: 'mainnet'
    baseUrl?: string;              // default: 'https://api.ston.fi'
    timeoutMs?: number;            // default: 12_000
    retries?: number;              // default: 2
}

export async function readPoolReserves(
    client: TonClient,
    tokenAMinter: Address,
    tokenBMinter: Address,
    POOL_ADDR: Address
): Promise<{ rPton: bigint; rUsdt: bigint; poolPtonWallet: Address | null; poolUsdtWallet: Address | null }> {
    const poolAWallet = await getJettonWalletOfOwner(client, tokenAMinter, POOL_ADDR);
    const poolBWallet = await getJettonWalletOfOwner(client, tokenBMinter, POOL_ADDR);

    let rPtonRaw: bigint | null = null;
    let rUsdtRaw: bigint | null = null;

    if (!poolAWallet) {
        console.warn(`Failed to obtain poolAWallet pool wallet for pool ${POOL_ADDR.toString()}; treating balance as 0n`);
    }
    if (!poolBWallet) {
        console.warn(`Failed to obtain poolBWallet pool wallet for pool ${POOL_ADDR.toString()}; treating balance as 0n`);
    }

    const balancePromises: Promise<void>[] = [];
    if (poolAWallet) {
        balancePromises.push(
            (async () => {
                rPtonRaw = await getJettonWalletBalance(client, poolAWallet!);
            })()
        );
    }
    if (poolBWallet) {
        balancePromises.push(
            (async () => {
                rUsdtRaw = await getJettonWalletBalance(client, poolBWallet!);
            })()
        );
    }
    if (balancePromises.length) {
        await Promise.all(balancePromises);
    }

    if (rPtonRaw === null && poolAWallet) console.warn(`PTON pool wallet ${poolAWallet.toString()} balance is null; treating as 0n`);
    if (rUsdtRaw === null && poolBWallet) console.warn(`USDT pool wallet ${poolBWallet.toString()} balance is null; treating as 0n`);

    const rPton = rPtonRaw ?? 0n;
    const rUsdt = rUsdtRaw ?? 0n;

    return { rPton, rUsdt, poolPtonWallet: poolAWallet, poolUsdtWallet: poolBWallet };
}

export async function getJettonWalletOfOwner(client: TonClient, minter: Address, owner: Address): Promise<Address | null> {
    try {
        const res = await client.runMethod(minter, "get_wallet_address", [
            { type: "slice", cell: beginCell().storeAddress(owner).endCell() },
        ]);
        const addr = res.stack.readAddressOpt?.() ?? res.stack.readAddress?.() ?? null;
        if (!addr) {
            console.warn(`No wallet address returned for minter ${minter.toString()} and owner ${owner.toString()}`);
            return null;
        }
        console.log(`Wallet for minter ${minter.toString()} and owner ${owner.toString()}: ${addr.toString()}`);
        return addr;
    } catch (e) {
        console.error(`Failed to get wallet address for minter ${minter.toString()} and owner ${owner.toString()}:`, e);
        return null;
    }
}

export async function getJettonWalletBalance(client: TonClient, wallet: Address): Promise<bigint | null> {
    try {
        const st = await getAccountState(client, wallet);
        if (!st || st.state !== "active") return 0n;
        const res = await client.runMethod(wallet, "get_balance", []);
        const num = res.stack.readBigNumber();
        console.log(`Wallet balance of ${wallet.toString()}: ${num}`);
        return num; // nano-jettons
    } catch (e) {
        console.error(`Failed to read wallet balance for ${wallet.toString()}:`, e);
        return null; // keep current behavior
    }
}

export async function getPoolAddressForPair(
    client: TonClient,
    routerAddr: Address,
    tokenWallet0: Address,
    tokenWallet1: Address
): Promise<Address | null> {
    try {
        const res = await client.runMethod(routerAddr, "get_pool_address", [
            { type: "slice", cell: beginCell().storeAddress(tokenWallet0).endCell() },
            { type: "slice", cell: beginCell().storeAddress(tokenWallet1).endCell() },
        ]);
        const addr = res.stack.readAddressOpt?.() ?? res.stack.readAddress?.() ?? null;
        if (!addr) {
            console.warn(`No pool address returned for token wallets ${tokenWallet0.toString()} and ${tokenWallet1.toString()}`);
            return null;
        }
        console.log(`Pool address for token wallets ${tokenWallet0.toString()} and ${tokenWallet1.toString()}: ${addr.toString()}`);
        return addr;
    } catch (e) {
        console.error(`Failed to read pool address from router ${routerAddr.toString()}:`, e);
        return null;
    }
}

export async function fetchStonPools(opts: FetchPoolsOptions = {}): Promise<StonPool[]> {
    const {
        network = "mainnet",
        baseUrl = "https://api.ston.fi",
        timeoutMs = 12_000,
        retries = 2,
    } = opts;

    const url = new URL("/v1/pools", baseUrl);
    url.searchParams.set("network", network);

    let lastErr: unknown;
    for (let attempt = 0; attempt <= retries; attempt++) {
        try {
            const res = await withTimeout(
                fetch(url.toString(), {
                    method: "GET",
                    headers: { accept: "application/json" },
                }),
                timeoutMs,
                new Error(`STON.fi /v1/pools timeout after ${timeoutMs}ms`)
            );

            if (!res.ok) {
                const text = await res.text().catch(() => "");
                throw new Error(`STON.fi /v1/pools HTTP ${res.status} ${res.statusText} — ${text}`);
            }

            const data = (await res.json()) as PoolListResponse;
            if (!data || !Array.isArray(data.pool_list)) {
                throw new Error("Unexpected STON.fi response shape: no pool_list");
            }
            return data.pool_list;
        } catch (e) {
            lastErr = e;
            if (attempt < retries) {
                await new Promise(r => setTimeout(r, 300 * (attempt + 1)));
            }
        }
    }
    throw lastErr instanceof Error ? lastErr : new Error(String(lastErr));
}
