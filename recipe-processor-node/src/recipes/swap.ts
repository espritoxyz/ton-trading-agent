import {TonClient, WalletContractV5R1} from "@ton/ton";
import {Address, internal, SendMode, toNano} from "@ton/core";
import {mnemonicToPrivateKey} from "@ton/crypto";
import {DEX} from "@ston-fi/sdk";
import {mnemonic_array} from "../mnemonics.js";
import {fetchStonRouters, pickBestStonfiPoolForPair} from "../stonfi/router.js";
import {readPoolsFromFile} from "../stonfi/poolsCache.js";
import {bufToHex, waitSeqno} from "../utils.js";

export interface SuccessReport {
    ok: true;
    userId: number;
    txId: string;
    router: string;     // friendly address
    pool: string;       // friendly address
    pTon: string;       // friendly address
    jettonMinter: string; // friendly address of requested jetton
    offerNanotons: string; // string to avoid bigint JSON issues
    minAskNano: string;    // string
}

export interface ErrorReport {
    ok: false;
    userId: number;
    error: string;
    details?: any;
}

const endpoint = process.env.TONCENTER_ENDPOINT || "https://toncenter.com/api/v2/jsonRPC";
const apiKey = process.env.TONCENTER_API_KEY || "";

export async function swapTonToToken(
    userId: number,
    jettonMaster: Address,
    minimalTokenAmount: number,
    swapTonAmount: number,
): Promise<SuccessReport | ErrorReport> {
    try {
        const client = new TonClient({ endpoint, apiKey });
        const { publicKey, secretKey } = await mnemonicToPrivateKey(mnemonic_array);
        const wallet = WalletContractV5R1.create({ publicKey, workchain: 0 });
        const provider = client.open(wallet);

        const offerTON = toNano(swapTonAmount);
        const minAsk = String(Math.max(0, Math.floor(minimalTokenAmount)));

        const [routerList, pools] = await Promise.all([
            fetchStonRouters(),
            Promise.resolve(readPoolsFromFile()),
        ]);
        if (!pools || (Array.isArray(pools) && pools.length === 0)) {
            return {
                ok: false,
                userId,
                error: "STON.fi pools cache unavailable",
                details: { hint: "Ensure pools updater is running and stonfi-pools.json exists" },
            };
        }

        const bestPool = pickBestStonfiPoolForPair(pools, routerList, jettonMaster, undefined, offerTON);
        if (!bestPool) {
            return {
                ok: false,
                userId,
                error: "No suitable pool found for TON -> target jetton",
                details: { jettonMaster: jettonMaster.toString({ bounceable: false }), offerTon: offerTON },
            };
        }

        const routerWrapper = DEX.v2_1.Router.CPI.create(bestPool.router);
        const routerOC = client.open(routerWrapper);
        const pTon = DEX.v2_1.pTON.create(bestPool.pTon);

        const txParams = await routerOC.getSwapTonToJettonTxParams({
            userWalletAddress: wallet.address,
            proxyTon: pTon,
            offerAmount: offerTON,
            askJettonAddress: jettonMaster, // jetton minter
            minAskAmount: minAsk,
        });

        const before = await provider.getSeqno();
        await provider.sendTransfer({
            seqno: before,
            secretKey,
            messages: [internal(txParams)],
            sendMode: SendMode.PAY_GAS_SEPARATELY,
        });

        await waitSeqno(provider, before);

        const txs = await client.getTransactions(wallet.address, { limit: 5 });
        const raw = typeof (txs[0] as any).hash === "function" ? (txs[0] as any).hash() : (txs[0] as any).hash;
        const txId = bufToHex(raw);

        return {
            ok: true,
            userId,
            txId,
            router: bestPool.router.toString({ bounceable: false }),
            pool: bestPool.pool.toString({ bounceable: false }),
            pTon: bestPool.pTon.toString({ bounceable: false }),
            jettonMinter: jettonMaster.toString({ bounceable: false }),
            offerNanotons: offerTON.toString(),
            minAskNano: minAsk,
        };
    } catch (e: any) {
        return {
            ok: false,
            userId,
            error: e?.message || "Swap failed",
            details: e?.response?.data ?? (e?.stack || String(e)),
        };
    }
}
